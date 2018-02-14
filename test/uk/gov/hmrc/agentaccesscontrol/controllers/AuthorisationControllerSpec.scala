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

package uk.gov.hmrc.agentaccesscontrol.controllers

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.mockito.Matchers.{eq => eqs, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.mvc.Http.Status
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.model.AuthPostDetails
import uk.gov.hmrc.agentaccesscontrol.service.{AuthorisationService, MtdItAuthorisationService, MtdVatAuthorisationService}
import uk.gov.hmrc.agentmtdidentifiers.model.{MtdItId, Vrn}
import uk.gov.hmrc.domain.{AgentCode, EmpRef, Nino, SaUtr}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

class AuthorisationControllerSpec extends UnitSpec with BeforeAndAfterEach with MockitoSugar {

  val auditService = mock[AuditService]
  val authorisationService = mock[AuthorisationService]
  val mtdItAuthorisationService = mock[MtdItAuthorisationService]
  val mtdVatAuthorisationService = mock[MtdVatAuthorisationService]
  def controller(enabled: Boolean = true) = new AuthorisationController(auditService, authorisationService, mtdItAuthorisationService, mtdVatAuthorisationService, Configuration("features.allowPayeAccess" -> enabled))

  private val saAuthPostJson = Json.toJson(Map("ggCredentialId" -> "some-ggId", "saAgentReference" -> "some-saAgentReference"))
  private val ePayeAuthPostJson = Json.toJson(Map("ggCredentialId" -> "some-ggId"))
  private val mtdAuthPostJson = Json.toJson(Map("arn" -> "some-arn"))

  implicit val system: ActorSystem = ActorSystem("sys")
  implicit val materializer: Materializer = ActorMaterializer()

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(auditService, authorisationService)
  }

  "POST isAuthorisedForSa" should {

    val fakeRequest = FakeRequest("POST", "/agent-access-control/sa-auth/agent//client/utr").withBody(saAuthPostJson)

    "return 401 if the AuthorisationService doesn't permit access" in {

      whenAuthorisationServiceIsCalled thenReturn(Future successful false)

      val response = controller().isAuthorisedForSa(AgentCode(""), SaUtr("utr"))(fakeRequest)

      status (response) shouldBe Status.UNAUTHORIZED
    }

    "return 200 if the AuthorisationService allows access" in {

      whenAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller().isAuthorisedForSa(AgentCode(""), SaUtr("utr"))(fakeRequest)

      status(response) shouldBe Status.OK
    }

    "pass request to AuthorisationService" in {

      whenAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller().isAuthorisedForSa(AgentCode(""), SaUtr("utr"))(fakeRequest)

      verify(authorisationService).isAuthorisedForSa(any[AgentCode], any[SaUtr], any[AuthPostDetails])(any[ExecutionContext], any[HeaderCarrier], eqs(fakeRequest))

      status(response) shouldBe Status.OK
    }

    "propagate exception if the AuthorisationService fails" in {

      whenAuthorisationServiceIsCalled thenReturn(Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(status(controller().isAuthorisedForSa(AgentCode(""), SaUtr("utr"))(fakeRequest)))
    }

  }

  "POST isAuthorisedForMtdIt" should {
    val fakeRequest = FakeRequest("POST", "/agent-access-control/mtd-it-auth/agent//client/utr").withBody(Json.toJson(mtdAuthPostJson))

    "return 401 if the MtdAuthorisationService doesn't permit access" in {

      whenMtdItAuthorisationServiceIsCalled thenReturn(Future successful false)

      val response = controller().isAuthorisedForMtdIt(AgentCode(""), MtdItId("mtdItId"))(fakeRequest)

      status (response) shouldBe Status.UNAUTHORIZED
    }


    "return 200 if the MtdAuthorisationService allows access" in {

      whenMtdItAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller().isAuthorisedForMtdIt(AgentCode(""), MtdItId("mtdItId"))(fakeRequest)

      status(response) shouldBe Status.OK
    }


    "propagate exception if the MtdAuthorisationService fails" in {

      whenMtdItAuthorisationServiceIsCalled thenReturn(Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(status(controller().isAuthorisedForMtdIt(AgentCode(""), MtdItId("mtdItId"))(fakeRequest)))
    }

  }

  "POST isAuthorisedForMtdVat" should {
    val fakeRequest = FakeRequest("POST", "/agent-access-control/mtd-vat-auth/agent//client/utr").withBody(Json.toJson(mtdAuthPostJson))

    "return 401 if the MtdVatAuthorisationService doesn't permit access" in {

      whenMtdVatAuthorisationServiceIsCalled thenReturn(Future successful false)

      val response = controller().isAuthorisedForMtdVat(AgentCode(""), Vrn("vrn"))(fakeRequest)

      status (response) shouldBe Status.UNAUTHORIZED
    }


    "return 200 if the MtdVatAuthorisationService allows access" in {

      whenMtdVatAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller().isAuthorisedForMtdVat(AgentCode(""), Vrn("vrn"))(fakeRequest)

      status(response) shouldBe Status.OK
    }


    "propagate exception if the MtdVatAuthorisationService fails" in {

      whenMtdVatAuthorisationServiceIsCalled thenReturn(Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(status(controller().isAuthorisedForMtdVat(AgentCode(""), Vrn("vrn"))(fakeRequest)))
    }
  }

  "POST isAuthorisedForPaye" should {
    val fakeRequest = FakeRequest("POST", "/agent-access-control/epaye-auth/agent//client/utr").withBody(Json.toJson(ePayeAuthPostJson))

    "return 200 when Paye is enabled" in {
      whenPayeAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller().isAuthorisedForPaye(AgentCode(""), EmpRef("123", "123456"))(fakeRequest)

      status(response) shouldBe 200
    }

    "return 403 when Paye is disabled" in {
      whenPayeAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller(enabled = false).isAuthorisedForPaye(AgentCode(""), EmpRef("123", "123456"))(fakeRequest)

      status(response) shouldBe 403
    }
  }

  "POST isAuthorisedForAfi" should {
    val fakeRequest = FakeRequest("POST", "/agent-access-control/afi-auth/agent//client/utr").withBody(Json.toJson(mtdAuthPostJson))

    "return 200 if the AuthorisationService allows access" in {

      whenAfiAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller().isAuthorisedForAfi(AgentCode(""), Nino("AA123456A"))(fakeRequest)

      status(response) shouldBe Status.OK
    }

    "return 404 if the AuthorisationService does not allow access" in {

      whenAfiAuthorisationServiceIsCalled thenReturn(Future successful false)

      val response = controller().isAuthorisedForAfi(AgentCode(""), Nino("AA123456A"))(fakeRequest)

      status(response) shouldBe Status.NOT_FOUND
    }


    "propagate exception if the AuthorisationService fails" in {

      whenAfiAuthorisationServiceIsCalled thenReturn(Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(status(controller().isAuthorisedForAfi(AgentCode(""), Nino("AA123456A"))(fakeRequest)))
    }
  }

  def whenAfiAuthorisationServiceIsCalled =
    when(authorisationService.isAuthorisedForAfi(any[AgentCode], any[Nino], any[AuthPostDetails])(any[ExecutionContext], any[HeaderCarrier], any[Request[Any]]))

  def whenAuthorisationServiceIsCalled =
    when(authorisationService.isAuthorisedForSa(any[AgentCode], any[SaUtr], any[AuthPostDetails])(any[ExecutionContext], any[HeaderCarrier], any[Request[Any]]))

  def whenMtdItAuthorisationServiceIsCalled =
    when(mtdItAuthorisationService.authoriseForMtdIt(any[AgentCode], any[MtdItId], any[AuthPostDetails])(any[ExecutionContext], any[HeaderCarrier], any[Request[_]]))

  def whenMtdVatAuthorisationServiceIsCalled =
    when(mtdVatAuthorisationService.authoriseForMtdVat(any[AgentCode], any[Vrn], any[AuthPostDetails])(any[ExecutionContext], any[HeaderCarrier], any[Request[_]]))

  def whenPayeAuthorisationServiceIsCalled =
    when(authorisationService.isAuthorisedForPaye(any[AgentCode], any[EmpRef], any[AuthPostDetails])(any[ExecutionContext], any[HeaderCarrier], any[Request[_]]))
}