/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.binders

import play.api.mvc.PathBindable
import uk.gov.hmrc.agentmtdidentifiers.model.{CgtRef, MtdItId, Utr, Vrn}
import uk.gov.hmrc.domain.{AgentCode, EmpRef, Nino, SaUtr}

object PathBinders {
  implicit object AgentCodeBinder
      extends SimpleObjectBinder[AgentCode](AgentCode.apply, _.value)

  implicit object SaUtrBinder
      extends SimpleObjectBinder[SaUtr](SaUtr.apply, _.value)

  implicit object MtdItIdBinder
      extends SimpleObjectBinder[MtdItId](MtdItId.apply, _.value)

  implicit object NinoBinder
      extends SimpleObjectBinder[Nino](Nino.apply, _.value)

  implicit object VrnBinder extends SimpleObjectBinder[Vrn](Vrn.apply, _.value)

  implicit object UtrBinder extends SimpleObjectBinder[Utr](Utr.apply, _.value)

  implicit object CgtBinder
      extends SimpleObjectBinder[CgtRef](CgtRef.apply, _.value)

  implicit object EmpRefBinder extends PathBindable[EmpRef] {

    def bind(key: String, value: String) =
      try {
        Right(EmpRef.fromIdentifiers(value))
      } catch {
        case e: IllegalArgumentException =>
          Left(s"Cannot parse parameter '$key' with value '$value' as EmpRef")
      }

    def unbind(key: String, empRef: EmpRef): String = empRef.encodedValue
  }
}
