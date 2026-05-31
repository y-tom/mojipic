package controllers

import java.nio.file.{FileSystems, Files, Path, StandardCopyOption}
import java.time.{Clock, LocalDateTime}
import javax.inject.{Inject, Singleton}

import com.google.common.net.MediaType
import com.redis.RedisClient
import domain.entity.{PictureProperty, TwitterId}
import domain.repository.PicturePropertyRepository
import infrastructure.redis.RedisKeys
import play.api.cache.SyncCacheApi
import play.api.libs.Files.TemporaryFile
import play.api.mvc._
import play.api.mvc.MultipartFormData.FilePart

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PicturesController @Inject()(
                                    cc: ControllerComponents,
                                    clock: Clock,
                                    executionContext: ExecutionContext,
                                    val cache: SyncCacheApi,
                                    picturePropertyRepository: PicturePropertyRepository,
                                    redisClient: RedisClient
                                  ) extends TwitterLoginController(cc) {

  implicit val ec: ExecutionContext = executionContext
  // アップロードされた元画像を保存するディレクトリ
  val originalStoreDirPath = "./filesystem/original"

  // 画像アップロード用のPOST処理
  def post: Action[AnyContent] = TwitterLoginAction.async { request

    // Twitterログイン情報とmultipart/form-dataがあるか確認
    (request.accessToken, request.body.asMultipartFormData) match {
      case (Some(accessToken), Some(form)) =>

        // formから画像ファイルを取得
        form.file("file") match {
          case Some(file) =>

            // 保存先ディレクトリを取得し、存在しなければ作成
            val storeDirPath = FileSystems.getDefault.getPath(originalStoreDirPath)
            if (!Files.exists(storeDirPath)) Files.createDirectories(storeDirPath)

            // 現在時刻をファイル名として、保存先パスを作成
            val originalFilepath =
              FileSystems.getDefault.getPath(storeDirPath.toString, System.currentTimeMillis().toString)

            // 一時ファイルから保存先へ画像をコピー
            Files.copy(file.ref.path, originalFilepath, StandardCopyOption.COPY_ATTRIBUTES)

            // DB保存用の画像プロパティを作成
            val propertyValue =
              createPicturePropertyValue(TwitterId(accessToken.getUserId), file, form, originalFilepath)

            // 作成したプロパティを確認用に出力
            val pictureId = picturePropertyRepository.create(propertyValue)
            pictureId.map({ (id) =>
              println(s"redis push id = ${id.value}")
              redisClient.rpush(RedisKeys.Tasks, id.value)
              Ok("Picture uploaded.")
            })
          // Future.successful(Ok("Picture uploaded."))ありの講義コードだとRedis登録前にレスポンスを返してしまうため削除
          // Future.successful(Ok("Picture uploaded."))

          // fileが送られていない場合
          case _ => Future.successful(Unauthorized("Need picture data."))


      // ログイン情報 or 画像データがない場合
      case _ => Future.successful(Unauthorized("Need to login by Twitter and picture data."))
    }
  }

  // フォーム情報からPictureProperty.Valueを作成する
  private[this] def createPicturePropertyValue(
                                                twitterId: TwitterId,
                                                file: FilePart[TemporaryFile],
                                                form: MultipartFormData[TemporaryFile],
                                                originalFilePath: Path
                                              ): PictureProperty.Value = {
    // 変換用テキストを取得。なければ空文字
    val overlayText =
      form.dataParts.get("overlaytext").flatMap(_.headOption).getOrElse("")

    // 変換用テキストサイズを取得。なければ60
    // 講義コードは.filter(_.nonEmpty)なし。空文字が送られるとtoIntで例外になるため追加
    val overlayTextSize =
      form.dataParts.get("overlaytextsize").flatMap(_.headOption).filter(_.nonEmpty).getOrElse("60").toInt

    // Content-Typeを取得。なければapplication/octet-stream
    val contentType =
      MediaType.parse(file.contentType.getOrElse("application/octet-stream"))

    // 画像のプロパティ値を作成
    PictureProperty.Value(
      PictureProperty.Status.Converting, // 初期状態は変換中
      twitterId,                         // 投稿者のTwitter ID
      file.filename,                     // 元のファイル名
      contentType,                       // Content-Type
      overlayText,                       // 変換用テキスト
      overlayTextSize,                   // 変換用テキストサイズ
      Some(originalFilePath.toString),   // 変換前画像の保存パス
      None,                              // 変換後画像はまだない
      LocalDateTime.now(clock)           // 投稿日時
    )
  }

}