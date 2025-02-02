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
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

trait ErrorResponseCope {
  def code: String
}

case class ErrorResponseCopeProcessing(
  code: String,
  copeDataAvailableDate: LocalDate,
  previousAvailableDate: Option[LocalDate] = None
) extends ErrorResponseCope

case class ErrorResponseCopeFailed(
  code: String
) extends ErrorResponseCope


object ErrorResponseCope {
  implicit val copeProcessingFormat: Format[ErrorResponseCopeProcessing] = Json.format[ErrorResponseCopeProcessing]
  implicit val copeFailedFormat: Format[ErrorResponseCopeFailed] = Json.format[ErrorResponseCopeFailed]
}
