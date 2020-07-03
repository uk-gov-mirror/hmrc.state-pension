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

import akka.util.Timeout
import org.mockito.Matchers.{any, eq => MockitoEq}
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{BAD_REQUEST, OK, UNAUTHORIZED}
import play.api.mvc.{Action, AnyContent, Controller, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import uk.gov.hmrc.auth.core.AuthProvider.{PrivilegedApplication, Verify}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.domain.Generator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class AuthActionSpec
  extends PlaySpec
    with GuiceOneAppPerSuite
    with BeforeAndAfter
    with MockitoSugar {

  class AuthActionTestHarness(authAction: AuthAction) extends Controller {
    def onPageLoad(): Action[AnyContent] = authAction { request =>
      Ok
    }
  }

  private val ninoGenerator = new Generator()
  private val testNino = ninoGenerator.nextNino.nino
  private val goodUriWithNino = s"/ni/$testNino/"

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
            testAuthActionWith(Future.successful(None))

          status(result) mustBe OK

          verify(mockAuthConnector)
            .authorise[Unit](MockitoEq(
              (ConfidenceLevel.L200 and Nino(true, Some(testNino))) or (ConfidenceLevel.L500 and AuthProviders(Verify)) or AuthProviders(PrivilegedApplication)
            ),
              any())(any(), any())
        }
        "the user is a trusted helper and requests with the nino of the helpee" in {
          val (result, mockAuthConnector) =
            testAuthActionWith(Future.successful(Some(TrustedHelper("", "", "", testNino))))

          status(result) mustBe OK

          verify(mockAuthConnector)
            .authorise[Unit](MockitoEq(
              (ConfidenceLevel.L200 and Nino(true, Some(testNino))) or (ConfidenceLevel.L500 and AuthProviders(Verify)) or AuthProviders(PrivilegedApplication)
            ),
              any())(any(), any())
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

        "the trusted helper nino does not match the uri Nino" in {
          val notTestNino = testNino.take(testNino.length-1) + "X"
          val (result, _) =
            testAuthActionWith(Future.successful(Some(TrustedHelper("", "", "", notTestNino))))
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
    }
  }

  private def newMockConnectorWithAuthResult[T](authoriseResult: Future[T]): MicroserviceAuthConnector = {
    val connector = mock[MicroserviceAuthConnector]

    when(connector.authorise[T](any(), any())(any(), any()))
      .thenReturn(authoriseResult)

    connector
  }

  private def testAuthActionWith[T](authResult: Future[T],
                                    uri: String = goodUriWithNino): (Future[Result], MicroserviceAuthConnector) = {
    val mockAuthConnector = newMockConnectorWithAuthResult(authResult)
    val authAction = new AuthActionImpl(mockAuthConnector)

    val testHarness = new AuthActionTestHarness(authAction)

    (testHarness.onPageLoad()(FakeRequest(method = "", path = uri)),
      mockAuthConnector)
  }
}

