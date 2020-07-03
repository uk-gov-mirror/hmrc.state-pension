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

package uk.gov.hmrc.statepension.controllers.auth

import com.google.inject.{ImplementedBy, Inject}
import play.api.Mode.Mode
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.AuthProvider.{PrivilegedApplication, Verify}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.statepension.WSHttp

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future.successful

class AuthActionImpl @Inject()(val authConnector: AuthConnector)(implicit executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions {

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)

    val matchNinoInUriPattern = "/ni/([^/]+)/?.*".r

    val matches = matchNinoInUriPattern.findAllIn(request.uri)

    if (matches.isEmpty) {
      successful(Some(BadRequest))
    } else {
      val uriNino: Option[String] = Some(matches.group(1))
      authorised(
        (ConfidenceLevel.L200 and Nino(true, uriNino)) or (ConfidenceLevel.L500 and AuthProviders(Verify)) or AuthProviders(PrivilegedApplication)
      ).retrieve(Retrievals.trustedHelper) {
        case Some(trusted) =>
          if (uriNino.contains(trusted.principalNino)) successful(None) else successful(Some(Unauthorized))
        case None => successful(None)
      } recover {
        case t: Throwable =>
          Logger.debug("Debug info - " + t.getMessage)
          Some(Unauthorized)
      }
    }
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[Request] with ActionFilter[Request]

class MicroserviceAuthConnector @Inject()(val http: WSHttp,
                                          val runModeConfiguration: Configuration,
                                          environment: Environment
                                         ) extends PlayAuthConnector with ServicesConfig {
  override val serviceUrl: String = baseUrl("auth")

  override protected def mode: Mode = environment.mode
}