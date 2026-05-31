import java.time.Clock

import com.google.inject.AbstractModule
import com.redis.RedisClient
import domain.repository.PicturePropertyRepository
import infrastructure.repository.PicturePropertyRepositoryImpl
import play.api.{Configuration, Environment}

// DIの設定を行うクラス
class Module(environment: Environment,
             configuration: Configuration) extends AbstractModule {

  // application.conf からRedisの接続情報を取得
  val redisHost: String = configuration.get[String]("mojipic.redis.host")
  val redisPort: Int = configuration.get[Int]("mojipic.redis.port")

  override def configure(): Unit = {
    // Clockが必要な場所には、現在時刻を扱うClockを渡す
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    // PicturePropertyRepositoryが必要な場所には、実装クラスを渡す
    bind(classOf[PicturePropertyRepository]).to(classOf[PicturePropertyRepositoryImpl])
    // RedisClientが必要な場所には、接続情報つきのインスタンスを渡す
    bind(classOf[RedisClient]).toInstance(new RedisClient(redisHost, redisPort))
  }
}