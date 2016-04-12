package client

import akka.actor._
import com.virtuslab.akkaworkshop.PasswordsDistributor._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class RequesterActor extends Actor {

  val worker = context.actorOf(Props[WorkerActor])

  private var token: String = _
  private var remoteActor: ActorSelection = _

  private val initialRequestCount = 5

  def requestPassword() = sender ! SendMeEncryptedPassword(token)

  // receive with messages that can be sent by the server
  override def receive: Receive = {

    case remote: ActorSelection =>
      remoteActor = remote
      remote ! Register("kromanowski")

    case Registered(newToken) =>
      token = newToken
      (1 to initialRequestCount).foreach(_ => requestPassword())

    case EncryptedPassword(encryptedPassword) =>
      worker ! WorkerRequest(self, encryptedPassword)

    case WorkerResult(original, decrypted) =>
      remoteActor ! ValidateDecodedPassword(token, original, decrypted)

    case PasswordCorrect(decryptedPassword) =>
      println("Success!")
      requestPassword()

    case PasswordIncorrect(decryptedPassword) =>
      println("Failure!")

      requestPassword()
  }

}

object RequesterActor {

  def props = Props[RequesterActor]

  // messages needed to communicate with the server
  def registerMessage(name : String) = Register(name)

  def validatePasswordMessage(token: Token,
                              encryptedPassword : String,
                              decryptedPassword : String) =
    ValidateDecodedPassword(token, encryptedPassword, decryptedPassword)



}

object AkkaApplication extends App {

  val system = ActorSystem("RequesterSystem")

  val requesterActor = system.actorOf(Props[RequesterActor])

  val remoteActor = system.actorSelection("akka.tcp://application@localhost:9552/user/PasswordsDistributor")

  requesterActor ! remoteActor

  Await.result(system.whenTerminated, Duration.Inf)

}
