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

package uk.gov.hmrc.statepension.domain.des

import org.joda.time.{LocalDate, Period}
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.math.BigDecimal.RoundingMode

case class DesSummary(
                       earningsIncludedUpTo: LocalDate,
                       sex: String,
                       statePensionAgeDate: LocalDate,
                       finalRelevantStartYear: Int,
                       pensionSharingOrderSERPS: Boolean,
                       dateOfBirth: LocalDate,
                       dateOfDeath: Option[LocalDate] = None,
                       reducedRateElection: Boolean = false,
                       countryCode: Int = 0,
                       amounts: NpsStatePensionAmounts = NpsStatePensionAmounts()
                     ) {
  val finalRelevantYear: String = s"$finalRelevantStartYear-${(finalRelevantStartYear + 1).toString.takeRight(2)}"
  val statePensionAge: Int = new Period(dateOfBirth, statePensionAgeDate).getYears
}

object DesSummary {

  val readBooleanFromInt: JsPath => Reads[Boolean] =
    jsPath => jsPath.readNullable[Int].map(_.getOrElse(0) != 0)

  val readNullableInt: JsPath => Reads[Int] =
    jsPath => jsPath.readNullable[Int].map(_.getOrElse(0))

  implicit val reads: Reads[DesSummary] = (
    (JsPath \ "earningsIncludedUpto").read[LocalDate] and
      (JsPath \ "sex").read[String] and
      (JsPath \ "spaDate").read[LocalDate] and
      (JsPath \ "finalRelevantYear").read[Int] and
      (JsPath \ "pensionShareOrderSerps").read[Boolean] and
      (JsPath \ "dateOfBirth").read[LocalDate] and
      (JsPath \ "dateOfDeath").readNullable[LocalDate] and
      (JsPath \ "reducedRateElectionToConsider").read[Boolean] and
      readNullableInt(JsPath \ "countryCode") and
      (JsPath \ "statePensionAmount").read[NpsStatePensionAmounts]
    ) (DesSummary.apply _)

}

case class NpsStatePensionAmounts(
                                   pensionEntitlement: BigDecimal = 0,
                                   startingAmount2016: BigDecimal = 0,
                                   protectedPayment2016: BigDecimal = 0,
                                   amountA2016: NpsAmountA2016 = NpsAmountA2016(),
                                   amountB2016: NpsAmountB2016 = NpsAmountB2016()
                                 ) {
  lazy val pensionEntitlementRounded: BigDecimal = pensionEntitlement.setScale(2, RoundingMode.HALF_UP)
}

object NpsStatePensionAmounts {

  val readBigDecimal: JsPath => Reads[BigDecimal] =
    jsPath => jsPath.readNullable[BigDecimal].map(_.getOrElse(0))

  implicit val reads: Reads[NpsStatePensionAmounts] = (
    readBigDecimal(JsPath \ "nspEntitlement") and
      readBigDecimal(JsPath \ "startingAmount") and
      readBigDecimal(JsPath \ "protectedPayment2016") and
      (JsPath \ "amountA2016").read[NpsAmountA2016] and
      (JsPath \ "amountB2016").read[NpsAmountB2016]
    ) (NpsStatePensionAmounts.apply _)
}

case class NpsAmountA2016(
                           basicStatePension: BigDecimal = 0,
                           pre97AP: BigDecimal = 0,
                           post97AP: BigDecimal = 0,
                           post02AP: BigDecimal = 0,
                           pre88GMP: BigDecimal = 0,
                           post88GMP: BigDecimal = 0,
                           pre88COD: BigDecimal = 0,
                           post88COD: BigDecimal = 0,
                           graduatedRetirementBenefit: BigDecimal = 0
                         ) {
   val additionalStatePension: BigDecimal = (pre97AP - (pre88GMP + post88GMP + pre88COD + post88COD)).max(0) + post97AP + post02AP

   val totalAP: BigDecimal = additionalStatePension + graduatedRetirementBenefit
   val total: BigDecimal = totalAP + basicStatePension

}

object NpsAmountA2016 {

  val readBigDecimal: JsPath => Reads[BigDecimal] =
    jsPath => jsPath.readNullable[BigDecimal].map(_.getOrElse(0))

  implicit val reads: Reads[NpsAmountA2016] = (
    readBigDecimal(JsPath \ "ltbCatACashValue") and
      readBigDecimal(JsPath \ "ltbPre97ApCashValaue") and
      readBigDecimal(JsPath \ "ltbPost97ApCashValue") and
      readBigDecimal(JsPath \ "ltbPost02ApCashValue") and
      readBigDecimal(JsPath \ "pre88Gmp") and
      readBigDecimal(JsPath \ "ltbPst88GmpCashValue") and
      readBigDecimal(JsPath \ "ltbPre88CodCashValue") and
      readBigDecimal(JsPath \ "ltbPost88CodCashValue") and
      readBigDecimal(JsPath \ "grbCash")
    ) (NpsAmountA2016.apply _)

}

case class NpsAmountB2016(mainComponent: BigDecimal = 0, rebateDerivedAmount: BigDecimal = 0)

object NpsAmountB2016 {
  val readBigDecimal: JsPath => Reads[BigDecimal] =
    jsPath => jsPath.readNullable[BigDecimal].map(_.getOrElse(0))

  implicit val reads: Reads[NpsAmountB2016] = (
    readBigDecimal(JsPath \ "mainComponent") and
      readBigDecimal(JsPath \ "rebateDerivedAmount")
    ) (NpsAmountB2016.apply _)
}
