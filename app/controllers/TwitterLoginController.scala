// === sessionId / accessToken を request にくっつける係 ===
package controllers

import java.util.UUID

import play.api.cache.SyncCacheApi
import play.api.mvc._
import twitter4j.auth.AccessToken

import scala.concurrent.{ExecutionContext, Future}

// 通常の Request に、sessionId と accessToken を追加したリクエスト
case class TwitterLoginRequest[A](
                                   sessionId: String,
                                   accessToken: Option[AccessToken],
                                   request: Request[A]
                                 ) extends WrappedRequest[A](request)

// Twitterログイン状態を扱う Controller の共通処理
abstract class TwitterLoginController(
                                       protected val cc: ControllerComponents
                                     ) extends AbstractController(cc) {

  // AccessToken を保存・取得するための cache
  val cache: SyncCacheApi

  // sessionId を保存する Cookie 名
  val sessionIdName = "mojipic.sessionId"

  // Twitterログイン用の独自 Action
  def TwitterLoginAction: ActionBuilder[TwitterLoginRequest, AnyContent] =
    new ActionBuilder[TwitterLoginRequest, AnyContent] {

      // Future を実行するための ExecutionContext
      override protected def executionContext: ExecutionContext =
        cc.executionContext

      // リクエストボディのパーサー
      override def parser: BodyParser[AnyContent] =
        cc.parsers.defaultBodyParser

      def invokeBlock[A](
                          request: Request[A],
                          block: TwitterLoginRequest[A] => Future[Result]
                        ): Future[Result] = {

        // Cookie から sessionId を取得
        val sessionIdOpt =
          request.cookies.get(sessionIdName).map(_.value)

        // sessionId があれば、cache から AccessToken を取得
        val accessToken =
          sessionIdOpt.flatMap(cache.get[AccessToken])

        // sessionId がなければ、新しく UUID で作成
        val sessionId =
          sessionIdOpt.getOrElse(UUID.randomUUID().toString)

        // sessionId / accessToken を追加した Request を Controller に渡す
        val result =
          block(TwitterLoginRequest(sessionId, accessToken, request))

        implicit val executionContext: ExecutionContext =
          cc.executionContext

        // レスポンスに sessionId Cookie を付けて返す
        result.map(
          _.withCookies(Cookie(sessionIdName, sessionId, Some(30 * 60)))
        )
      }
    }
}