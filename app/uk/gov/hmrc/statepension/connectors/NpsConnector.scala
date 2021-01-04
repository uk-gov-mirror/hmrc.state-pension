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

package uk.gov.hmrc.statepension.connectors

import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, Reads}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.statepension.WSHttp
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.services.ApplicationMetrics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait NpsConnector {

  val http: WSHttp
  val metrics: ApplicationMetrics
  val token: String
  val serviceOriginatorId: (String, String)
  val environmentHeader: (String, String)
  def summaryUrl(nino: Nino): String
  def liabilitiesUrl(nino: Nino): String
  def niRecordUrl(nino: Nino): String

  val summaryMetricType: APIType
  val liabilitiesMetricType: APIType
  val niRecordMetricType: APIType

  def getSummary(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[Summary] =
    connectToHOD[Summary](summaryUrl(nino), summaryMetricType)

  def getLiabilities(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[List[Liability]] =
    connectToHOD[Liabilities](liabilitiesUrl(nino), liabilitiesMetricType).map(_.liabilities)

  def getNIRecord(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[NIRecord] =
    connectToHOD[NIRecord](niRecordUrl(nino), niRecordMetricType)

  private def connectToHOD[A](url: String, api: APIType)(implicit headerCarrier: HeaderCarrier, reads: Reads[A]): Future[A] = {
    val timerContext = metrics.startTimer(api)
    val responseF = http.GET[HttpResponse](url)(HttpReads.readRaw, HeaderCarrier(Some(Authorization(s"Bearer $token")))
      .withExtraHeaders(serviceOriginatorId, environmentHeader),  ec=global)

    responseF.map { httpResponse =>
      timerContext.stop()
      Try(httpResponse.json.validate[A]).flatMap( jsResult =>
        jsResult.fold(errs => Failure(new JsonValidationException(formatJsonErrors(errs))), valid => Success(valid))
      )
    } recover {
      // http-verbs throws exceptions, convert to Try
      case ex => Failure(ex)
    } flatMap (handleResult(api, url, _))
  }

  //TODO[Refactor] url is not used
  private def handleResult[A](api: APIType, url: String, tryResult: Try[A]): Future[A] = {
    tryResult match {
      case Failure(ex) =>
        metrics.incrementFailedCounter(api)
        Future.failed(ex)
      case Success(value) =>
        Future.successful(value)
    }
  }

  private def formatJsonErrors(errors: Seq[(JsPath, Seq[ValidationError])]): String = {
    errors.map(p => p._1 + " - " + p._2.map(_.message).mkString(",")).mkString(" | ")
  }

  class JsonValidationException(message: String) extends Exception(message)
}

