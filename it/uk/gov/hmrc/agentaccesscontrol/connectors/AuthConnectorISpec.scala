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

import uk.gov.hmrc.agentaccesscontrol.WSHttp
import uk.gov.hmrc.agentaccesscontrol.support.BaseISpec
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class AuthConnectorISpec extends BaseISpec {

  "getSaAgentReference" should {
    "return sa agent reference" in {
      implicit val hc = given()
        .agentAdmin("ABCDEF123456").isLoggedIn()
        .andHasSaAgentReference("REF879")
        .headerCarrier

      await(newAuthConnector.currentSaAgentReference) shouldBe Some(SaAgentReference("REF879"))
    }

    "return None if 6 digit agent reference cannot be found" in {
      implicit val hc = given()
        .agentAdmin("ABCDEF123456").isLoggedIn()
        .andHasNoSaAgentReference()
        .headerCarrier

      await(newAuthConnector.currentSaAgentReference) shouldBe None
    }

    "return None if no user is logged in" in {
      implicit val hc = HeaderCarrier()

      await(newAuthConnector.currentSaAgentReference) shouldBe None
    }

    "return a failed future if any errors happen" in {
      implicit val hc = given()
        .agentAdmin("ABCDEF123456").isLoggedIn()
        .andGettingEnrolmentsFailsWith500()
        .headerCarrier

      an[Exception] shouldBe thrownBy {
        await(newAuthConnector.currentSaAgentReference)
      }
    }

    def newAuthConnector() = new AuthConnector(new URL(wiremockBaseUrl), WSHttp)
  }

}
