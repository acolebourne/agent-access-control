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

package uk.gov.hmrc.agentaccesscontrol.service

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.agentaccesscontrol.connectors.AuthConnector
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

class SaAgentAuthorisationServiceSpec extends UnitSpec with MockitoSugar {
  val agentCode = AgentCode("ABCDEF123456")
  val saAgentRef = SaAgentReference("ABC456")
  val clientSaUtr = SaUtr("CLIENTSAUTR456")


  implicit val headerCarrier = HeaderCarrier()

  "isAuthorised" should {
    "return true if the Agent has an IR-SA-AGENT enrolment" in new Context {
      when(mockAuthConnector.hasActivatedIrSaEnrolment()).thenReturn(true)

      await(authorisationService.isAuthorised()) shouldBe true
    }

    "return false if the Agent has an IR-SA-AGENT enrolment" in new Context {
      when(mockAuthConnector.hasActivatedIrSaEnrolment()).thenReturn(false)

      await(authorisationService.isAuthorised()) shouldBe false
    }
  }

  private abstract class Context {
    val mockAuthConnector = mock[AuthConnector]
    val authorisationService = new SaAgentAuthorisationService(
      mockAuthConnector)
  }
}
