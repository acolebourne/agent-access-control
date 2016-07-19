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

package uk.gov.hmrc.agentaccesscontrol

import uk.gov.hmrc.agentaccesscontrol.support.{BaseISpec, Resource}
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, SaUtr}
import uk.gov.hmrc.play.http.HttpResponse

class SaAgentAuthorisationControllerISpec extends BaseISpec {

  val agentCode = AgentCode("ABCDEF123456")
  val saAgentReference = SaAgentReference("ABC456")
  val clientUtr = SaUtr("123")

  "/agent-access-control/sa-agent/agent/:agentCode/client/:saUtr" should {
    "respond with 401" when {
      "agent is not logged in" in {
        given()
          .agentAdmin(agentCode).isNotLoggedIn()

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }

      "agent is not enrolled to IR-SA-AGENT" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andIsNotEnrolledForSA()

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }

      "agent is enrolled to IR-SA-AGENT but the enrolment is not activated" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasIrSaEnrolment(enrolmentState = pendingEnrolmentState)

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }
    }

    "respond with 200" when {
      "agent has an activated enrolment to IR-SA-AGENT" in  {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasIrSaEnrolment(enrolmentState = activatedEnrolmentState)

        authResponseFor(agentCode, clientUtr).status shouldBe 200
      }
    }

  }
  
  def authResponseFor(agentCode: AgentCode, clientSaUtr: SaUtr): HttpResponse =
    new Resource(s"/agent-access-control/sa-agent/agent/${agentCode.value}/client/${clientSaUtr.value}")(port).get()

}
