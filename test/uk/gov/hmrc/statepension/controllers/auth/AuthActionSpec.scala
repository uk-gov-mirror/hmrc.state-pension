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

package uk.gov.hmrc.statepension.controllers.auth

import akka.util.Timeout
import org.mockito.ArgumentMatchers.{any, eq => MockitoEq}
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.status
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.auth.core.retrieve.{GGCredId, PAClientId, ~}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.statepension.controllers.auth.AuthActionSpec.retrievalsTestingSyntax

import scala.concurrent.Future
import scala.concurrent.duration._

class AuthActionSpec
  extends PlaySpec
    with GuiceOneAppPerSuite
    with BeforeAndAfter
    with MockitoSugar {

  val controllerComponents = Helpers.stubControllerComponents()

  class AuthActionTestHarness(authAction: AuthAction) extends BackendController(controllerComponents) {
    def onPageLoad(): Action[AnyContent] = authAction { request =>
      Ok
    }
  }

  private val ninoGenerator: Generator = new Generator()
  private val testNino: String = ninoGenerator.nextNino.nino
  private val goodUriWithNino: String = s"/ni/$testNino/"

  implicit val timeout: Timeout = 5 seconds

  "Auth Action" when {
    "the user is not logged in" must {
      "return UNAUTHORIZED" in {
        val (result, _) =
          testAuthActionWith(Future.failed(new MissingBearerToken))
        status(result) mustBe UNAUTHORIZED
      }
    }

    "the user is logged in" must {
      "return the request" when {
        "the user is authorised and Nino matches the Nino in the uri" in {

          val (result, mockAuthConnector) =
            testAuthActionWith(Future.successful(Some(testNino) ~ None  ~ GGCredId("")))

          status(result) mustBe OK

          verify(mockAuthConnector)
            .authorise[Unit](MockitoEq(
              ConfidenceLevel.L200 or AuthProviders(PrivilegedApplication)
            ), any())(any(), any())
        }
        "the user is a trusted helper and requests with the nino of the helpee" in {
          val helperNino = ninoGenerator.nextNino.nino
          val (result, mockAuthConnector) =
            testAuthActionWith(Future.successful(Some(helperNino) ~ Some(TrustedHelper("", "", "", testNino)) ~ GGCredId("")))

          status(result) mustBe OK

          verify(mockAuthConnector)
            .authorise[Unit](MockitoEq(
              ConfidenceLevel.L200 or AuthProviders(PrivilegedApplication)
            ), any())(any(), any())
        }

        "the request comes from a privileged application" in {
          val (result, mockAuthConnector) =
            testAuthActionWith(Future.successful(None ~ None ~ PAClientId("")))

          status(result) mustBe OK

          verify(mockAuthConnector)
            .authorise[Unit](MockitoEq(
              ConfidenceLevel.L200 or AuthProviders(PrivilegedApplication)
            ), any())(any(), any())
        }
      }

      "return UNAUTHORIZED" when {
        "the Confidence Level is less than 200" in {
          val (result, _) =
            testAuthActionWith(Future.failed(new InsufficientConfidenceLevel))
          status(result) mustBe UNAUTHORIZED
        }

        "the Nino is rejected by auth" in {
          val (result, _) =
            testAuthActionWith(Future.failed(InternalError("IncorrectNino")))
          status(result) mustBe UNAUTHORIZED
        }

        "not a Privileged application" in {
          val (result, _) =
            testAuthActionWith(Future.failed(new UnsupportedAuthProvider))
          status(result) mustBe UNAUTHORIZED
        }

        "the trusted helpee nino does not match the uri Nino" in {
          val notTestNino = testNino.take(testNino.length-1) + "X"
          val helperNino = ninoGenerator.nextNino.nino
          val (result, _) = testAuthActionWith(Future.successful(Some(helperNino) ~ Some(TrustedHelper("", "", "", notTestNino)) ~ GGCredId("")))
          status(result) mustBe UNAUTHORIZED
        }
      }

      "return BAD_REQUEST" when {
        "the user is authorised and the uri doesn't match our expected format" in {
          val (result, _) =
            testAuthActionWith(Future.successful(()),
              "/UriThatDoesNotMatchTheRegex")
          status(result) mustBe BAD_REQUEST
        }
      }
      "return INTERNAL_SERVER_ERROR" when {
        "auth returns with no nino" in {
          val (result, _) = testAuthActionWith(Future.successful(None ~ None))
          status(result) mustBe INTERNAL_SERVER_ERROR
        }

        "auth returns an unexpected exception" in {
          val (result, _) = testAuthActionWith(Future.failed(new Exception("")))
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  private def newMockConnectorWithAuthResult[T](authoriseResult: Future[T]): AuthConnector = {
    val connector = mock[AuthConnector]

    when(connector.authorise[T](any(), any())(any(), any()))
      .thenReturn(authoriseResult)

    connector
  }

  private def testAuthActionWith[T](authResult: Future[T],
                                    uri: String = goodUriWithNino): (Future[Result], AuthConnector) = {
    val mockAuthConnector = newMockConnectorWithAuthResult(authResult)

    val injector = new GuiceApplicationBuilder()
      .overrides(bind[AuthConnector].toInstance(mockAuthConnector))
      .injector()

    val authAction = injector.instanceOf[AuthAction]

    val testHarness = new AuthActionTestHarness(authAction)

    (testHarness.onPageLoad()(FakeRequest(method = "", path = uri)),
      mockAuthConnector)
  }

}

object AuthActionSpec {
  implicit class retrievalsTestingSyntax[A](val a: A) extends AnyVal {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }
}
