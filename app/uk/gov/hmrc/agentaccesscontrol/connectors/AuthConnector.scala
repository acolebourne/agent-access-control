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
import uk.gov.hmrc.agentaccesscontrol.model.{AuthEnrolment, Enrolments}
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpReads}

import scala.concurrent.{ExecutionContext, Future}


class AuthConnector(baseUrl: URL, httpGet: HttpGet) {

  def currentSaAgentReference()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SaAgentReference]] =
    currentAuthority
      .flatMap(enrolments)
      .map(toSaAgentReference)

  private def toSaAgentReference(enrolments: Option[Enrolments]): Option[SaAgentReference] =
    enrolments.flatMap(_.saAgentReferenceOption)

  private def currentAuthority()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[JsValue]] =
    toFutureOfOption(hc.userId.map(userId => httpGetAs[JsValue](userId.value)))

  private def enrolments(maybeAuthorityJson: Option[JsValue])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Enrolments]] =
    toFutureOfOption(maybeAuthorityJson.map { authorityJson =>
      httpGetAs[Set[AuthEnrolment]](enrolmentsRelativeUrl(authorityJson)).map(Enrolments(_))
    })

  private def toFutureOfOption[A](option: Option[Future[A]])(implicit ec: ExecutionContext): Future[Option[A]] = option match {
    case None => Future.successful(None)
    case Some(a) => a map(Some(_))
  }

  private def enrolmentsRelativeUrl(authorityJson: JsValue) = (authorityJson \ "enrolments").as[String]

  private def url(relativeUrl: String): URL = new URL(baseUrl, relativeUrl)

  private def httpGetAs[T](relativeUrl: String)(implicit rds: HttpReads[T], hc: HeaderCarrier, ec: ExecutionContext): Future[T] =
    httpGet.GET[T](url(relativeUrl).toString)(rds, hc)

}