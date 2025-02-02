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

import com.google.inject.{ImplementedBy, Inject}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import uk.gov.hmrc.api.controllers.{ErrorGenericBadRequest, ErrorInternalServerError, ErrorNotFound, ErrorResponse}
import uk.gov.hmrc.http.UpstreamErrorResponse.{Upstream4xxResponse, Upstream5xxResponse, WithStatusCode}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.ExclusionFormats._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@ImplementedBy(classOf[CopeErrorHandling])
trait ErrorHandling {
  self: BackendController =>

  val app: String = "State-Pension"

  def errorWrapper(func: => Future[Result])(implicit hc: HeaderCarrier): Future[Result]
}

class CopeErrorHandling @Inject()(cc: ControllerComponents, appConfig: AppConfig) extends BackendController(cc)
  with ErrorHandling with Logging {

  override def errorWrapper(func: => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    func.recover {
      case WithStatusCode(NOT_FOUND, _)       => NotFound(Json.toJson[ErrorResponse](ErrorNotFound))
      case _: NotFoundException               => NotFound(Json.toJson[ErrorResponse](ErrorNotFound))
      case WithStatusCode(GATEWAY_TIMEOUT, e) => logger.error(s"$app Gateway Timeout: ${e.getMessage}", e); GatewayTimeout
      case WithStatusCode(BAD_GATEWAY, e)     => logger.error(s"$app Bad Gateway: ${e.getMessage}", e); BadGateway
      case WithStatusCode(BAD_REQUEST, _)     => BadRequest(Json.toJson(ErrorGenericBadRequest("Upstream Bad Request. Is this customer below State Pension Age?")))
      case WithStatusCode(UNPROCESSABLE_ENTITY, e) if appConfig.copeFeatureEnabled && e.message.contains("NO_OPEN_COPE_WORK_ITEM") =>
        Forbidden(Json.toJson(ErrorResponses.ExclusionCopeProcessing(appConfig)))
      case WithStatusCode(UNPROCESSABLE_ENTITY, e) if appConfig.copeFeatureEnabled && e.message.contains("CLOSED_COPE_WORK_ITEM") =>
        Forbidden(Json.toJson[ErrorResponseCopeFailed](ErrorResponses.ExclusionCopeProcessingFailed))
      case Upstream4xxResponse(e) => logger.error(s"$app Upstream4XX: ${e.getMessage}", e); BadGateway
      case Upstream5xxResponse(e) => logger.error(s"$app Upstream5XX: ${e.getMessage}", e); BadGateway
      case e: Throwable =>
        logger.error(s"$app Internal server error: ${e.getMessage}", e)
        InternalServerError(Json.toJson(ErrorInternalServerError))
    }
  }
}
