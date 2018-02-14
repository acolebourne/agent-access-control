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
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.RelationshipsConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.{AuthConnector, AuthDetails}
import uk.gov.hmrc.agentaccesscontrol.model.AuthPostDetails
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.domain.AgentCode

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class MtdItAuthorisationService @Inject() (authConnector: AuthConnector,
                              relationshipsConnector: RelationshipsConnector,
                              auditService: AuditService) extends LoggingAuthorisationResults {

  def authoriseForMtdIt(agentCode: AgentCode, mtdItId: MtdItId, authPostDetails: AuthPostDetails)
                    (implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[_]): Future[Boolean] = {
    authPostDetails match {
      case AuthPostDetails(_,_, Some(arn), _) => {
        hasRelationship(Arn(arn), mtdItId) map { relationshipExists =>
          auditDecision(agentCode, authPostDetails, mtdItId, relationshipExists, "arn" -> arn)
          if (relationshipExists) authorised(s"Access allowed for agentCode=$agentCode arn=${arn} client=${mtdItId.value}")
          else notAuthorised(s"Access not allowed for agentCode=$agentCode arn=${arn} client=${mtdItId.value}")
        }
      }
      case AuthPostDetails(None, None, None, _) => {
        Future successful notAuthorised("No user is logged in")
      }
      case _ => {
        auditDecision(agentCode, authPostDetails, mtdItId, result = false)
        Future successful notAuthorised(s"No ARN found in HMRC-AS-AGENT enrolment for agentCode $agentCode")
      }
    }
  }

  private def hasRelationship(arn: Arn, mtdItId: MtdItId)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] = {
    relationshipsConnector.relationshipExists(arn, mtdItId)
  }

  private def auditDecision(
                             agentCode: AgentCode, agentAuthDetails: AuthPostDetails, mtdItId: MtdItId,
                             result: Boolean, extraDetails: (String, Any)*)
                           (implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] = {

    auditService.auditEvent(
      AgentAccessControlEvent.AgentAccessControlDecision,
      "agent access decision",
      agentCode,
      "mtd-it",
      mtdItId,
      Seq("credId" -> agentAuthDetails.ggCredentialId.getOrElse(""),
        "accessGranted" -> result)
        ++ extraDetails)
  }
}
