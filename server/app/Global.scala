import play.api._
import play.api.libs.concurrent.Akka
import akka.actor.ActorRef

import com.virtuslab.akkaworkshop._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    val distributor = Akka.system(app).actorOf(PasswordsDistributor.props, name = "PasswordsDistributor")
    testDistributor(app, distributor)
  }

  private def testDistributor(app: Application, distributor: ActorRef) {
    import PasswordsDistributor._
    import org.apache.commons.codec.binary.Base64
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration._
    import scala.concurrent.Await
    import akka.pattern.ask
    import akka.util.Timeout
    import scala.util.Random

    implicit val timeout = Timeout(5.seconds)

    def updateClient(token: String) {
      val encryptedPassword = Await.result(distributor ? SendMeEncryptedPassword(token), timeout.duration).
        asInstanceOf[EncryptedPassword].encryptedPassword

      def decrypt(p: String) = new String(Base64.decodeBase64(p.getBytes))
      val decryptedPassword = decrypt(decrypt(decrypt(encryptedPassword)))

      if (Random.nextBoolean()) distributor ! ValidateDecodedPassword(token, encryptedPassword, decryptedPassword)
      else distributor ! ValidateDecodedPassword(token, encryptedPassword, encryptedPassword)
    }

    List("kuki") map {
      name =>
        val token = Await.result(distributor ? Register(name), timeout.duration).asInstanceOf[Registered].token
        Akka.system(app).scheduler.schedule(0.seconds, (2 + Random.nextInt(5)).seconds)(updateClient(token))
    }
  }
}