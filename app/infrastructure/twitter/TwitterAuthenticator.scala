// === Twitterとやり取りする係 ===
package infrastructure.twitter

import javax.inject.Inject

import play.api.Configuration
import play.api.cache.SyncCacheApi
import twitter4j.{Twitter, TwitterFactory}
import twitter4j.auth.AccessToken

import scala.concurrent.duration._
import scala.util.control.NonFatal

class TwitterAuthenticator @Inject() (
                                       configuration: Configuration,
                                       cache: SyncCacheApi
                                     ) {

  // cache に保存するときのキーの先頭部分
  val CacheKeyPrefixTwitter = "twitterInstance"

  // application.conf から Twitter API のキーを取得
  val ConsumerKey: String = configuration.get[String]("mojipic.consumerkey")
  val ConsumerSecret: String = configuration.get[String]("mojipic.consumersecret")

  // sessionId ごとに cache 用のキーを作る
  private[this] def cacheKeyTwitter(sessionId: String): String =
    CacheKeyPrefixTwitter + sessionId

  /**
   * Twitterの認証を開始する
   * @param sessionId Twitterの認証をしたいセッションID
   * @param callbackUrl コールバックURL
   * @return 投稿者に認証してもらうためのURL
   * @throws TwitterException 何らかの理由でTwitterの認証を開始できなかった
   */
  def startAuthentication(sessionId: String, callbackUrl: String): String =
    try {
      // Twitter4J の Twitter インスタンスを作成
      val twitter = new TwitterFactory().getInstance()

      // ConsumerKey / ConsumerSecret をセット
      twitter.setOAuthConsumer(
        ConsumerKey,
        ConsumerSecret
      )

      // callbackUrl を渡して、リクエストトークンを取得
      val requestToken = twitter.getOAuthRequestToken(callbackUrl)

      // 次の getAccessToken で使うために Twitter インスタンスを一時保存
      cache.set(cacheKeyTwitter(sessionId), twitter, 30.seconds)

      // 投稿者に認証してもらうための URL を返す
      requestToken.getAuthenticationURL
    } catch {
      case NonFatal(e) =>
        // 認証開始に失敗したら独自例外に包んで投げる
        throw TwitterException(s"Could not get a request token. SessionId: $sessionId", e)
    }

  /**
   * Twitterのアクセストークンを取得する
   * @param sessionId Twitterの認証をしたいセッションID
   * @param verifier OAuth Verifier
   * @return アクセストークン
   * @throws TwitterException 何らかの理由でTwitterのアクセストークンを取得できなかった
   */
  def getAccessToken(sessionId: String, verifier: String): AccessToken =
    try {
      // startAuthentication で cache に保存した Twitter インスタンスを取得
      // verifier を使って、アクセストークンを取得
      cache.get[Twitter](cacheKeyTwitter(sessionId)).get.getOAuthAccessToken(verifier)
    } catch {
      case NonFatal(e) =>
        // アクセストークン取得に失敗したら独自例外に包んで投げる
        throw TwitterException(s"Could not get an access token. SessionId: $sessionId", e)
    }
}

// Twitter認証まわりで失敗したときに使う独自例外
case class TwitterException(message: String = null, cause: Throwable = null)
  extends RuntimeException(message, cause)