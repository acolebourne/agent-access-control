package uk.gov.hmrc.agentaccesscontrol

import play.api.libs.json.Json
import play.utils.UriEncoding.encodePathSegment
import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.AgentAccessControlDecision
import uk.gov.hmrc.agentaccesscontrol.stubs.DataStreamStub
import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportServerPerTest, Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.domain.{AgentCode, EmpRef}
import uk.gov.hmrc.http.HttpResponse

class PayeAuthorisationISpec extends WireMockWithOneServerPerTestISpec with MetricTestSupportServerPerTest {
  val agentCode = AgentCode("A11112222A")
  val empRef = EmpRef("123", "123456")

  "POST /agent-access-control/epaye-auth/agent/:agentCode/client/:empRef" should {
    val method = "POST"
    "return 200 when access is granted" in {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasNoIrSaAgentEnrolment()
        .andIsAssignedToClient(empRef)
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()

      val status = authResponseFor(agentCode, empRef).status

      status shouldBe 200
    }

    "return 401 when access is not granted" in {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasNoIrSaAgentEnrolment()
        .andIsNotAssignedToClient(empRef)
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()

      val status = authResponseFor(agentCode, empRef).status

      status shouldBe 401
    }

    "return 502 if a downstream service fails" in {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasNoIrSaAgentEnrolment()
        .andIsAssignedToClient(empRef)
        .andDesIsDown()

      val status = authResponseFor(agentCode, empRef).status

      status shouldBe 502
    }

    "record metrics for inbound http call" ignore {
      given()
        .agentAdmin(agentCode).isLoggedIn()
        .andHasNoIrSaAgentEnrolment()
        .andIsAssignedToClient(empRef)
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()
      givenCleanMetricRegistry()

      authResponseFor(agentCode, empRef).status shouldBe 200
      timerShouldExistsAndBeenUpdated("API-Agent-PAYE-Access-Control-GET")
    }

    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode).isLoggedIn()
        .andHasNoIrSaAgentEnrolment()
        .andIsAssignedToClient(empRef)
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()

      authResponseFor(agentCode, empRef).status shouldBe 200

      DataStreamStub.verifyAuditRequestSent(
        AgentAccessControlDecision,
        Map("path" -> s"/agent-access-control/epaye-auth/agent/$agentCode/client/${encodePathSegment(empRef.value, "UTF-8")}"))
    }
  }

  def authResponseFor(agentCode: AgentCode, empRef: EmpRef): HttpResponse = {
    new Resource(s"/agent-access-control/epaye-auth/agent/${agentCode.value}/client/${encodePathSegment(empRef.value, "UTF-8")}")(port).post(body = Json.toJson(Map("ggCredentialId" -> "0000001232456789", "affinityGroup" -> "Agent")))
  }


}