/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.services

import org.joda.time.LocalDate
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.domain.Exclusion

class ExclusionServiceSpec extends StatePensionUnitSpec {

  val exampleNow = new LocalDate(2017, 2, 16)
  val examplePensionDate = new LocalDate(2022, 2, 2)

  def exclusionServiceBuilder(dateOfDeath: Option[LocalDate] = None, pensionDate: LocalDate = examplePensionDate, now: LocalDate = exampleNow) =
    new ExclusionService(dateOfDeath, pensionDate, now)


  "getExclusions" when {
    "there is no exclusions" should {
      "return an empty list" in {
        exclusionServiceBuilder(dateOfDeath = None).getExclusions shouldBe Nil
      }
    }

    "there is a date of death" should {
      "return a List(Dead)" in {
        exclusionServiceBuilder(dateOfDeath = Some(new LocalDate(2000, 9, 13))).getExclusions shouldBe List(Exclusion.Dead)
      }
    }

    "checking for post state pension age" should {
      "return a List(PostStatePensionAge)" when {
        "the state pension age is the same as the current date" in {
          exclusionServiceBuilder(pensionDate = new LocalDate(2000, 1, 1), now = new LocalDate(2000, 1, 1)).getExclusions shouldBe List(Exclusion.PostStatePensionAge)
        }
        "the state pension age is one day after the the current date" in {
          exclusionServiceBuilder(pensionDate = new LocalDate(2000, 1, 2), now = new LocalDate(2000, 1, 1)).getExclusions shouldBe List(Exclusion.PostStatePensionAge)
        }
        "the state pension age is one day before the the current date" in {
          exclusionServiceBuilder(pensionDate = new LocalDate(2000, 1, 1), now = new LocalDate(2000, 1, 2)).getExclusions shouldBe List(Exclusion.PostStatePensionAge)
        }
      }

      "return an empty list" when {
        "the state pension age is two days after the current date" in {
          exclusionServiceBuilder(pensionDate = new LocalDate(2000, 1, 3), now = new LocalDate(2000, 1, 1)).getExclusions shouldBe List()
        }
      }
    }

    "all the exclusion criteria are met" should {
      exclusionServiceBuilder(dateOfDeath = Some(new LocalDate(1999, 12, 31)), pensionDate = new LocalDate(2000, 1, 1), now = new LocalDate(2000, 1, 1)).getExclusions shouldBe List(Exclusion.Dead, Exclusion.PostStatePensionAge)
    }
  }

}
