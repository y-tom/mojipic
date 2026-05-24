// === login / callback / logout の画面操作を担当する係 ===
package controllers

import javax.inject.Inject
import infrastructure.twitter.{TwitterAuthenticator, TwitterException}
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.mvc.{Action, AnyContent, ControllerComponents}

import scala.concurrent.duration._

class OAuthController @Inject()(
                                 cc: ControllerComponents,
                                 twitterAuthenticator: TwitterAuthenticator,
                                 configuration: Configuration,
                                 val cache: SyncCacheApi
                               ) extends TwitterLoginController(cc) {

  // application.conf からアプリのルートURLを取得
  val documentRootUrl: String =
    configuration.get[String]("mojipic.documentrooturl")

  // Twitterログイン開始
  def login: Action[AnyContent] = TwitterLoginAction { request =>
    try {
      // Twitter認証後に戻ってくるURLを作る
      val callbackUrl =
        documentRootUrl + routes.OAuthController.oauthCallback(None).url

      // Twitterの認証URLを取得
      val authenticationUrl =
        twitterAuthenticator.startAuthentication(request.sessionId, callbackUrl)

      // Twitterの認証画面へリダイレクト
      Redirect(authenticationUrl)
    } catch {
      case e: TwitterException =>
        // 認証開始に失敗した場合
        BadRequest(e.message)
    }
  }

  // Twitter認証後のコールバック
  def oauthCallback(verifierOpt: Option[String]): Action[AnyContent] =
    TwitterLoginAction { request =>
      try {
        // verifier があれば、それを使って AccessToken を取得
        verifierOpt.map(
          twitterAuthenticator.getAccessToken(request.sessionId, _)
        ) match {

          case Some(accessToken) =>
            // AccessToken を sessionId をキーにして cache に保存
            cache.set(request.sessionId, accessToken, 30.minutes)

            // ログイン後、トップページへリダイレクト
            Redirect(documentRootUrl + routes.HomeController.index().url)

          case None =>
            // verifier が取れなかった場合
            BadRequest(
              s"Could not get OAuth verifier. SessionId: ${request.sessionId}"
            )
        }
      } catch {
        case e: TwitterException =>
          // AccessToken 取得に失敗した場合
          BadRequest(e.message)
      }
    }

  // ログアウト
  def logout: Action[AnyContent] = TwitterLoginAction { request =>
    // sessionId に紐づく AccessToken を cache から削除
    cache.remove(request.sessionId)

    // トップページへリダイレクト
    Redirect(documentRootUrl + routes.HomeController.index().url)
  }
}