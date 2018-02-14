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

package uk.gov.hmrc.agentaccesscontrol.model

case class AuthPostDetails(ggCredentialId: Option[String], saAgentReference: Option[String], arn: Option[String], affinityGroup: Option[String])

//trait AuthPostDetails{
//  val affinityGroup: Option[String]
//}
//
//case class MtdAuthDetails(override val affinityGroup: Option[String], arn: Option[String]) extends AuthPostDetails
//case class PayeAuthDetails(override val affinityGroup: Option[String], ggCredentialId: Option[String]) extends AuthPostDetails
//case class SaAuthDetails(override val affinityGroup: Option[String], ggCredentialId: Option[String], saAgentReference: Option[String]) extends AuthPostDetails

