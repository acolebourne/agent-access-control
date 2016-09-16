/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.connectors

import java.net.URL

import play.api.libs.json.JsValue
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentaccesscontrol.model.{AuthEnrolment, Enrolments}
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpReads, Upstream4xxResponse}

import scala.concurrent.{ExecutionContext, Future}


class AuthConnector(baseUrl: URL, httpGet: HttpGet) extends HttpAPIMonitor {

  def currentAgentIdentifiers()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[(Option[SaAgentReference], String)]] =
    currentAuthority
      .flatMap({ authority =>
        monitor("ConsumedAPI-AUTH-GetEnrolments-GET") { enrolments(authority) }
          .map(e => toSaAgentReference(e))
          .map(ref => Some((ref, ggCredentialId(authority))))
      }) recover {
      case ex: Upstream4xxResponse if ex.upstreamResponseCode == 401 => None
    }


  private def ggCredentialId(authorityJson: JsValue): String = {
    (authorityJson \ "credentials" \ "gatewayId").as[String]
  }

  private def toSaAgentReference(enrolments: Enrolments): Option[SaAgentReference] =
    enrolments.saAgentReferenceOption

  private def currentAuthority()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsValue] =
    httpGetAs[JsValue]("/auth/authority")

  private def enrolments(authorityJson: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Enrolments] =
    httpGetAs[Set[AuthEnrolment]](enrolmentsRelativeUrl(authorityJson)).map(Enrolments(_))

  private def enrolmentsRelativeUrl(authorityJson: JsValue) = (authorityJson \ "enrolments").as[String]

  private def url(relativeUrl: String): URL = new URL(baseUrl, relativeUrl)

  private def httpGetAs[T](relativeUrl: String)(implicit rds: HttpReads[T], hc: HeaderCarrier, ec: ExecutionContext): Future[T] =
    httpGet.GET[T](url(relativeUrl).toString)(rds, hc)

}
