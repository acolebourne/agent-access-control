package uk.gov.hmrc.agentaccesscontrol

import play.api.libs.json.Json
import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.AgentAccessControlDecision
import uk.gov.hmrc.agentaccesscontrol.stubs.DataStreamStub
import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportServerPerTest, Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Vrn}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.http.HttpResponse

class MtdVatAuthorisationISpec extends WireMockWithOneServerPerTestISpec with MetricTestSupportServerPerTest {
  val agentCode = AgentCode("A11112222A")
  val arn = Arn("01234567890")
  val vrn = Vrn("12345677890")

  "POST /agent-access-control/mtd-vat-auth/agent/:agentCode/client/:mtdItId" ignore {
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasHmrcAsAgentEnrolment(arn)
      given().mtdAgency(arn)
        .hasARelationshipWith(vrn)

      val status = authResponseFor(agentCode, vrn).status

      status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        given().agentAdmin(agentCode).isLoggedIn()
          .andHasNoHmrcAsAgentEnrolment()

        val status = authResponseFor(agentCode, vrn).status

        status shouldBe 401
      }

      "there is no relationship between the agency and client" in {
        given().agentAdmin(agentCode).isLoggedIn()
          .andHasHmrcAsAgentEnrolment(arn)
        given().mtdAgency(arn)
          .hasNoRelationshipWith(vrn)

        val status = authResponseFor(agentCode, vrn).status

        status shouldBe 401
      }
    }

    "send an AccessControlDecision audit event" in {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasHmrcAsAgentEnrolment(arn)
      given().mtdAgency(arn)
        .hasARelationshipWith(vrn)

      authResponseFor(agentCode, vrn).status shouldBe 200

      DataStreamStub.verifyAuditRequestSent(
        AgentAccessControlDecision,
        Map("path" -> s"/agent-access-control/mtd-vat-auth/agent/$agentCode/client/${vrn.value}"))
    }

    "record metrics for access control request" in {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasHmrcAsAgentEnrolment(arn)
      given().mtdAgency(arn)
        .hasARelationshipWith(vrn)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, vrn).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-Agent-MTD-VAT-Access-Control-GET")
    }
  }

  def authResponseFor(agentCode: AgentCode, vrn: Vrn): HttpResponse = {
    new Resource(s"/agent-access-control/mtd-vat-auth/agent/${agentCode.value}/client/${vrn.value}")(port).post(body = Json.toJson(Map("arn" -> arn.value, "affinityGroup" -> "Agent")))
  }
}
