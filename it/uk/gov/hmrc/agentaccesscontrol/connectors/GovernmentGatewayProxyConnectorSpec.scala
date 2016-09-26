package uk.gov.hmrc.agentaccesscontrol.connectors

import java.net.{URI, URL}

import com.kenshoo.play.metrics.MetricsRegistry
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.verify
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.agentaccesscontrol.WSHttp
import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.GGW_Response
import uk.gov.hmrc.agentaccesscontrol.audit.{AuditService, RequestResponseToBeAudited}
import uk.gov.hmrc.agentaccesscontrol.support.BaseISpec
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream5xxResponse}

import scala.xml.SAXParseException

class GovernmentGatewayProxyConnectorSpec extends BaseISpec with MockitoSugar {

  val auditService = mock[AuditService]
  val agentCode = AgentCode("AgentCode")
  val connector = new GovernmentGatewayProxyConnector(new URL(wiremockBaseUrl), WSHttp, auditService)

  "GovernmentGatewayProxy" should {
    "return agent allocations and assignments" in {
      given()
        .agentAdmin("AgentCode", "000000123245678900")
          .andIsAllocatedAndAssignedToClient(SaUtr("1234567890"))

      val (allocation, _) = await(connector.getAssignedSaAgents(new SaUtr("1234567890"), agentCode))

      val details: AssignedAgent = allocation.head
      details.allocatedAgentCode shouldBe AgentCode("AgentCode")

      val credentials = details.assignedCredentials.head
      credentials.identifier shouldBe "000000123245678900"

      val credentials1 = details.assignedCredentials(1)
      credentials1.identifier shouldBe "98741987654321"

      val details1: AssignedAgent = allocation(1)
      details1.allocatedAgentCode shouldBe AgentCode("123ABCD12345")

      val credentials2 = details1.assignedCredentials.head
      credentials2.identifier shouldBe "98741987654322"
      verify(auditService).auditEvent(Matchers.eq(GGW_Response),
                                      Matchers.eq(agentCode),
                                      Matchers.eq(SaUtr("1234567890")),
                                      any[Seq[(String,Any)]])(any[HeaderCarrier])
    }

    "return empty list if there are no allocated agencies nor assigned credentials" in {
      given()
        .agentAdmin("AgentCode")
        .andIsNotAllocatedToClient(SaUtr("1234567890"))

      val (allocation, _) = await(connector.getAssignedSaAgents(new SaUtr("1234567890"), agentCode))

      allocation shouldBe empty
    }

    "throw exception for invalid XML" in {
      given()
        .agentAdmin("AgentCode")
        .andGovernmentGatewayReturnsUnparseableXml("1234567890")

      an[SAXParseException] should be thrownBy await(connector.getAssignedSaAgents(new SaUtr("1234567890"), agentCode))
    }

    "throw exception when HTTP error" in {
      given()
        .agentAdmin("AgentCode")
        .andGovernmentGatewayProxyReturnsAnError500()

      an[Upstream5xxResponse] should be thrownBy await(connector.getAssignedSaAgents(new SaUtr("1234567890"), agentCode))
    }

    "record metrics for outbound call" in {
      val metricsRegistry = MetricsRegistry.defaultRegistry
      given()
        .agentAdmin("AgentCode")
        .andIsAllocatedAndAssignedToClient(SaUtr("1234567890"))

      await(connector.getAssignedSaAgents(new SaUtr("1234567890"), agentCode))
      metricsRegistry.getTimers().get("Timer-ConsumedAPI-GGW-GetAssignedAgents-POST").getCount should be >= 1L
    }

    "return auditing info" in {
      given()
        .agentAdmin("AgentCode", "000000123245678900")
          .andIsAllocatedAndAssignedToClient(SaUtr("1234567890"))

      val (allocation: Seq[AssignedAgent], auditInfo: RequestResponseToBeAudited) =
        await(connector.getAssignedSaAgents(new SaUtr("1234567890"), agentCode))

      // request.detail.path in implicit events is not just the path - it is an absolute URL
      // we want to include the whole absolute URL in our explicit events to be consistent with implicit events
      val uri = new URI(auditInfo.request.detail("path"))
      uri.getScheme should not be empty
      uri.getHost should not be empty
      uri.getPath shouldBe "/government-gateway-proxy/api/admin/GsoAdminGetAssignedAgents"

      auditInfo.request.detail("method") shouldBe "POST"

      auditInfo.request.detail("X-Request-ID") should not be empty
    }
  }
}
