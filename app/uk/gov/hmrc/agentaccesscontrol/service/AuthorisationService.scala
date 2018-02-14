/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentaccesscontrol.service

import javax.inject.{Inject, Singleton}

import play.api.mvc.Request
import uk.gov.hmrc.agentaccesscontrol.audit.{AgentAccessControlEvent, AuditService}
import uk.gov.hmrc.agentaccesscontrol.connectors.{AfiRelationshipConnector, AuthConnector, AuthDetails}
import uk.gov.hmrc.agentaccesscontrol.model.AuthPostDetails
import uk.gov.hmrc.domain._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class AuthorisationService @Inject()(desAuthorisationService: DesAuthorisationService,
                                     authConnector: AuthConnector,
                                     espAuthorisationService: EnrolmentStoreProxyAuthorisationService,
                                     auditService: AuditService,
                                     afiRelationshipConnector: AfiRelationshipConnector)
  extends LoggingAuthorisationResults {

  private val accessGranted = true
  private val accessDenied = false

  def isAuthorisedForSa(agentCode: AgentCode, saUtr: SaUtr, postDetails: AuthPostDetails)
                       (implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] = {
    postDetails match {
      case AuthPostDetails(Some(ggCredentialId), Some(saAgentReference), _, _) => {
        for {
          isAuthorisedInESP <- espAuthorisationService.isAuthorisedForSaInEnrolmentStoreProxy(ggCredentialId, saUtr)
          maybeCesa <- checkCesaIfNecessary(isAuthorisedInESP, agentCode, SaAgentReference(saAgentReference), saUtr)
        } yield {
          val result = isAuthorisedInESP && maybeCesa.get

          val cesaDescription = desResultDescription(maybeCesa)
          auditDecision(agentCode, postDetails, "sa", saUtr, result, "cesaResult" -> cesaDescription, "enrolmentStoreResult" -> isAuthorisedInESP)

          if (result) authorised(s"Access allowed for agentCode=$agentCode ggCredential=${postDetails.ggCredentialId} client=$saUtr")
          else notAuthorised(s"Access not allowed for agentCode=$agentCode ggCredential=${postDetails.ggCredentialId} client=$saUtr esp=$isAuthorisedInESP cesa=$cesaDescription")
        }
      }
      case AuthPostDetails(None, None, None, _) => {
        Future successful notAuthorised("No user is logged in")
      }
      case _ => {
        auditDecision(agentCode, postDetails, "sa", saUtr, result = false)
        Future successful notAuthorised(s"No 6 digit agent reference found for agent $agentCode")
      }
    }
  }

  private def checkCesaIfNecessary(isAuthorisedInESP: Boolean, agentCode: AgentCode, saAgentReference: SaAgentReference, saUtr: SaUtr)
                                  (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[Boolean]] =
    if (isAuthorisedInESP) desAuthorisationService.isAuthorisedInCesa(agentCode, saAgentReference, saUtr).map(Some.apply)
    else Future successful None


  def isAuthorisedForPaye(agentCode: AgentCode, empRef: EmpRef, authPostDetails: AuthPostDetails)
                         (implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] = {
    authPostDetails match {
      case AuthPostDetails(Some(ggCredentialId), _, _, _) => {
        for {
          isAuthorisedInESP <- espAuthorisationService.isAuthorisedForPayeInEnrolmentStoreProxy(ggCredentialId, empRef)
          maybeEbs <- checkEbsIfNecessary(isAuthorisedInESP, agentCode, empRef)
        } yield {
          val result = isAuthorisedInESP && maybeEbs.get

          val ebsDescription = desResultDescription(maybeEbs)
          auditDecision(agentCode, authPostDetails, "paye", empRef, result, "ebsResult" -> ebsDescription, "enrolmentStoreResult" -> isAuthorisedInESP)

          if (result) authorised(s"Access allowed for agentCode=$agentCode ggCredential=${authPostDetails.ggCredentialId} client=$empRef")
          else notAuthorised(s"Access not allowed for agentCode=$agentCode ggCredential=${authPostDetails.ggCredentialId} client=$empRef esp=$isAuthorisedInESP ebs=$ebsDescription")
        }
      }
      case AuthPostDetails(None, None, None, _) => Future successful notAuthorised("No user is logged in")
      case _ => Future successful notAuthorised("ggCredentialId is not provided") //FIXME re-visit error message
    }
  }

  private def checkEbsIfNecessary(isAuthorisedInESP: Boolean, agentCode: AgentCode, empRef: EmpRef)
                                 (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[Boolean]] =
    if (isAuthorisedInESP) desAuthorisationService.isAuthorisedInEbs(agentCode, empRef).map(Some.apply)
    else Future successful None

  private def desResultDescription(maybeEbs: Option[Boolean]): Any = {
    maybeEbs.getOrElse("notChecked")
  }

  def isAuthorisedForAfi(agentCode: AgentCode, nino: Nino, authPostDetails: AuthPostDetails)
                        (implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] = {
    authPostDetails match {
      case AuthPostDetails(_,_, Some(arn), _) => {
        afiRelationshipConnector.hasRelationship(arn, nino.value) map { hasRelationship =>
          if (hasRelationship) {
            auditDecision(agentCode, authPostDetails, "afi", nino, accessGranted, "" -> "")
            found("Relationship Found")
          } else {
            auditDecision(agentCode, authPostDetails, "afi", nino, accessDenied, "" -> "")
            notFound("No relationship found")
          }
        }
      }
      case AuthPostDetails(None, None, None, _) => Future successful notFound("Error retrieving arn")
      case _ => Future successful notAuthorised("arn is not provided") //FIXME re-visit error message

    }
  }


  private def auditDecision(
                             agentCode: AgentCode, agentAuthDetails: AuthPostDetails, regime: String, taxIdentifier: TaxIdentifier,
                             result: Boolean, extraDetails: (String, Any)*)
                           (implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] = {
    val optionalDetails = Seq(
      agentAuthDetails.saAgentReference.map("saAgentReference" -> _),
      agentAuthDetails.affinityGroup.map("affinityGroup" -> _),
      Some("admin").map("agentUserRole" -> _) //TODO [APB-2030][HO,AK] hardcoded for now until we are able to get value from AUTH. (collaborating with team auth)
    ).flatten

    auditService.auditEvent(
      AgentAccessControlEvent.AgentAccessControlDecision,
      "agent access decision",
      agentCode,
      regime,
      taxIdentifier,
      Seq("credId" -> agentAuthDetails.ggCredentialId.getOrElse(""),
        "accessGranted" -> result)
        ++ extraDetails
        ++ optionalDetails)
  }
}
