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

package uk.gov.hmrc.statepension.events

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.statepension.domain.StatePensionAmounts

object StatePension{
  def apply(nino: Nino, earningsIncludedUpTo: LocalDate, amounts: StatePensionAmounts, pensionAge: Int,
            pensionDate: LocalDate, finalRelevantYear: String, numberOfQualifyingYears: Int, pensionSharingOrder: Boolean,
            currentFullWeeklyPensionAmount: BigDecimal)(implicit hc: HeaderCarrier): StatePension =
    new StatePension(nino, earningsIncludedUpTo, amounts, pensionAge, pensionDate, finalRelevantYear, numberOfQualifyingYears,
      pensionSharingOrder, currentFullWeeklyPensionAmount)
}

class StatePension(nino: Nino, earningsIncludedUpTo: LocalDate, amounts: StatePensionAmounts, pensionAge: Int,
                        pensionDate: LocalDate, finalRelevantYear: String, numberOfQualifyingYears: Int,
                        pensionSharingOrder: Boolean, currentFullWeeklyPensionAmount: BigDecimal) (implicit hc: HeaderCarrier)
  extends BusinessEvent("StatePension",
    Map("nino" -> nino.value,
      "earningsIncludedUpTo" -> earningsIncludedUpTo.toString,
      "currentAmount" -> amounts.current.toString,
      "forecastAmount" -> amounts.forecast.toString,
      "maximumAmount" -> amounts.maximum.toString,
      "copeAmount" -> amounts.cope.toString,
      "pensionAge" -> pensionAge.toString,
      "pensionDate" -> pensionDate.toString,
      "finalRelevantYear" -> finalRelevantYear,
      "numberOfQualifyingYears" -> numberOfQualifyingYears.toString,
      "pensionSharingOrder" -> pensionSharingOrder.toString,
      "currentFullWeeklyPensionAmount" -> currentFullWeeklyPensionAmount.toString()
    )
  )
