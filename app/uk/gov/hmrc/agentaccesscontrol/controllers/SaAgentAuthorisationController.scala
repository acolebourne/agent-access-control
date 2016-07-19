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

package uk.gov.hmrc.agentaccesscontrol.controllers

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import uk.gov.hmrc.agentaccesscontrol.service.SaAgentAuthorisationService
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.microservice.controller.BaseController

class SaAgentAuthorisationController(saAgentAuthorisationService: SaAgentAuthorisationService) extends BaseController {

  /**
    * This implements a delegated auth rule that just checks that the current
    * user an activated IR-SA-AGENT enrolment.
    *
    * It is used to check that the current user is an agent enrolled for SA
    * without checking that they are associated with any given client.
    *
    * Usually you want the sa-auth rule instead, which does check for an
    * association with a specific client. This auth rule was developed for
    * use during the agent-client authorisation process, when no agent-client
    * relationship will usually exist (because the whole point of the process
    * is to *create* such a relationship).
    */
  def isAuthorised(agentCode: AgentCode, saUtr: SaUtr) = Action.async { implicit request =>
    saAgentAuthorisationService.isAuthorised().map {
      case authorised if authorised => Ok
      case notAuthorised => Unauthorized
    } recover {
      case e =>
        Logger.error(s"Failed to check authorisation for $agentCode and $saUtr", e)
        Unauthorized
    }
  }
}
