/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.controllers

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.api.controllers.ErrorResponse
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

object ErrorResponses {
  val CODE_INVALID_NINO = "ERROR_NINO_INVALID"
  val CODE_MANUAL_CORRESPONDENCE = "EXCLUSION_MANUAL_CORRESPONDENCE"
  val CODE_DEAD = "EXCLUSION_DEAD"
  val CODE_COPE = "EXCLUSION_COPE"
  val CODE_COPE_2 = "EXCLUSION_COPE_2"

  object ExclusionCope2 extends CopeSecondError(CODE_COPE_2)
  object ExclusionCope extends ErrorResponseCope(CODE_COPE, LocalDate.now(), Some(LocalDate.now().plusDays(4)))
  object ErrorNinoInvalid extends ErrorResponse(400, CODE_INVALID_NINO, "The provided NINO is not valid")
  object ExclusionManualCorrespondence extends ErrorResponse(403, CODE_MANUAL_CORRESPONDENCE, "The customer cannot access the service, they should contact HMRC")
  object ExclusionDead extends ErrorResponse(403, CODE_DEAD, "The customer needs to contact the National Insurance helpline")
}