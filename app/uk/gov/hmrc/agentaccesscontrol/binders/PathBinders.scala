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

package uk.gov.hmrc.agentaccesscontrol.binders

import play.api.mvc.PathBindable
import uk.gov.hmrc.agentaccesscontrol.model.EmpRef
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.play.binders.SimpleObjectBinder

import scala.util.Try

object PathBinders {

  implicit object EmpRefBinder extends PathBindable[EmpRef] {

    def bind(key: String, value: String): Either[String, EmpRef] = {

      lazy val onError = Left(s"Cannot parse parameter '$key' with value '$value' as EmpRef")


      Try(Right(EmpRef.fromIdentifiers(value))).toOption.fold[Either[String, EmpRef]](onError)(identity)
    }

    def unbind(key: String, empRef: EmpRef): String = empRef.encodedValue
  }


  implicit object AgentCodeBinder extends SimpleObjectBinder[AgentCode](AgentCode.apply, _.value)
}
