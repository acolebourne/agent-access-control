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

package uk.gov.hmrc.agentaccesscontrol.model

import org.scalatest.{Matchers, WordSpec}

class EmpRefSpec extends WordSpec with Matchers {

  "Creating a new EmpRef from a single string" should {

    new ValidationTestCases {

      private def fromIdentifiers(taxOfficeNumber: String, taxOfficeReference: String): EmpRef = {
        // Treat null as "missing without slash"
        (Option(taxOfficeNumber), Option(taxOfficeReference)) match {
          case (None, None) => EmpRef.fromIdentifiers("")
          case (Some(num), None) => EmpRef.fromIdentifiers(num)
          case (None, Some(ref)) => EmpRef.fromIdentifiers(ref)
          case (Some(num), Some(ref)) => EmpRef.fromIdentifiers(s"$num/$ref")
        }
      }

      override def expectSuccess(taxOfficeNumber: String, taxOfficeReference: String): Unit = {
        fromIdentifiers(taxOfficeNumber, taxOfficeReference) shouldBe EmpRef(taxOfficeNumber, taxOfficeReference)
      }

      override def expectFailure(taxOfficeNumber: String, taxOfficeReference: String): Unit = {
        an [IllegalArgumentException] should be thrownBy fromIdentifiers(taxOfficeNumber, taxOfficeReference)
      }

      override def successAction: String = "create the expected EmpRef"
      override def failureAction: String = "throw an IllegalArgumentException"

    }.run()
  }

  "The isValid method" should {

    new ValidationTestCases {

      override def expectSuccess(taxOfficeNumber: String, taxOfficeReference: String): Unit = {
        EmpRef.isValid(taxOfficeNumber, taxOfficeReference) shouldBe true
      }

      override def expectFailure(taxOfficeNumber: String, taxOfficeReference: String): Unit = {
        EmpRef.isValid(taxOfficeNumber, taxOfficeReference) shouldBe false
      }

      override val successAction = "return true"
      override val failureAction = "return false"

    }.run()
  }

  "Constructing an EmpRef" should {

    new ValidationTestCases {

      override def expectSuccess(taxOfficeNumber: String, taxOfficeReference: String): Unit = {
        EmpRef(taxOfficeNumber, taxOfficeReference).value shouldBe s"$taxOfficeNumber/$taxOfficeReference"
      }

      override def expectFailure(taxOfficeNumber: String, taxOfficeReference: String): Unit = {
        an [IllegalArgumentException] should be thrownBy EmpRef(taxOfficeNumber, taxOfficeReference)
      }

      override def successAction: String = "create the expected EmpRef"
      override def failureAction: String = "throw an IllegalArgumentException"

    }.run()
  }

  private trait ValidationTestCases {

    def expectSuccess(taxOfficeNumber: String, taxOfficeReference: String): Unit

    def expectFailure(taxOfficeNumber: String, taxOfficeReference: String): Unit

    def failureAction: String
    def successAction: String

    def run() {
      s"$failureAction if the tax office number is null" in {
        expectFailure(null, "ABC123")
      }

      s"$failureAction if the tax office number is empty" in {
        expectFailure("", "ABC123")
      }

      s"$failureAction if the tax office number is blank" in {
        expectFailure("   ", "ABC123")
      }

      s"$failureAction if the tax office number has leading or trailing spaces" in {
        expectFailure(" 123", "ABC123")
        expectFailure("123 ", "ABC123")
        expectFailure(" 123 ", "ABC123")
      }

      s"$failureAction if the tax office number is longer than 3 digits" in {
        expectFailure("0000", "ABC123")
        expectFailure("0005", "ABC123")
        expectFailure("1000", "ABC123")
        expectFailure("12345", "ABC123")
      }

      s"$failureAction if the tax office number contains non-numeric characters" in {
        expectFailure("12A", "ABC123")
        expectFailure("1/A", "ABC123")
        expectFailure("AAA", "ABC123")
        expectFailure("1_2", "ABC123")
        expectFailure("1+2", "ABC123")
        expectFailure("1-2", "ABC123")
        expectFailure("1.2", "ABC123")
        expectFailure("-11", "ABC123")
      }

      s"$failureAction if the tax office number is shorter than 3 digits" in {
        expectFailure("73", "ABC123")
        expectFailure("7", "ABC123")
      }

      s"$failureAction if the reference is null" in {
        expectFailure("073", null)
      }

      s"$failureAction if the reference is empty" in {
        expectFailure("073", "")
      }

      s"$failureAction if the reference is blank" in {
        expectFailure("073", "   ")
      }

      s"$failureAction if the reference has leading or trailing whitespace" in {
        expectFailure("073", " ABC123")
        expectFailure("073", "ABC123 ")
        expectFailure("073", " ABC123 ")
      }

      s"$failureAction if the reference has any characters other than 0-9 and A-Z (uppercase)" in {
        expectFailure("073", "ABc123")
        expectFailure("073", "ABC_123")
        expectFailure("073", "ABC 123")
      }

      s"$failureAction if there is a forward slash in either the tax office number or tax office reference" in {
        expectFailure("7/3", "ABC123")
        expectFailure("073", "ABC/123")
      }

      s"$successAction if both the tax office number and reference are valid" in {
        expectSuccess("000", "A1B2C4D5")
        expectSuccess("073", "ABC123")
        expectSuccess("333", "123")
        expectSuccess("999", "A1B2C4D5")
        expectSuccess("999", "A")
      }
    }
  }
}
