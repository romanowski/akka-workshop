package com.virtuslab.akkaworkshop

import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging
import scala.util.Random
import org.apache.commons.codec.binary.Base64

object PasswordsDistributor {
  def props = Props[PasswordsDistributor]

  type Token = String

  sealed trait PDMessageRequest
  case class Register(name: String) extends PDMessageResponse
  case class SendMeEncryptedPassword(token: Token) extends PDMessageResponse
  case class ValidateDecodedPassword(token: Token,
                                     encryptedPassword: String,
                                     decryptedPassword: String) extends PDMessageResponse

  sealed trait PDMessageResponse
  case class Registered(token: Token)
  case class EncryptedPassword(encryptedPassword: String) extends PDMessageResponse
  case class PasswordCorrect(decryptedPassword: String) extends PDMessageResponse
  case class PasswordIncorrect(decryptedPassword: String) extends PDMessageResponse

  sealed trait PDMessageInternal
  case object SendMeStatistics extends PDMessageInternal
  case class Statistics(clients: Seq[Client])

}

class Client(val name: String) {
  val registrationTimestamp = timestamp
  var lastActionTimestamp = timestamp
  var passwordsRequested = 0
  var passwordsDecrypted = 0
  var passwordsInvalid = 0

  def passwordsPerMinute = (passwordsDecrypted.toFloat / ((timestamp - registrationTimestamp).toFloat / 60)).toInt

  def precentOfCorrect = ((100.0 * passwordsDecrypted) / passwordsRequested).toInt

  def timeFromLastAction = timestamp - lastActionTimestamp

  def timestamp = System.currentTimeMillis / 1000

  def isActive = (timestamp - lastActionTimestamp) < 60

  def updateTimestamp() {
    lastActionTimestamp = timestamp
  }
}

class PasswordsDistributor extends Actor {
  import PasswordsDistributor._

  val log = Logging(context.system, this)
  var clients = scala.collection.immutable.HashMap.empty[Token, Client]

  def randomString(n: Int) = new String(Seq.fill(n)(Random.nextPrintableChar).toArray)

  def receive = {
    case Register(name) =>
      val token = randomString(32)
      val client = new Client(name)
      client.updateTimestamp()
      clients = clients + ((token, client))
      sender ! Registered(token)

    case SendMeEncryptedPassword(token) =>
      def encrypt(password: String) = new String(Base64.encodeBase64(password.getBytes))

      clients.get(token) foreach {
        client =>
          val decrypted = randomString(8 + Random.nextInt(8))
          val encrypted = encrypt(encrypt(encrypt(decrypted)))
          client.updateTimestamp()
          client.passwordsRequested += 1
          sender ! EncryptedPassword(encrypted)
      }

    case ValidateDecodedPassword(token, encrypted, decrypted) =>
      def decrypt(password: String) = new String(Base64.decodeBase64(password.getBytes))

      clients.get(token) foreach {
        client =>
          client.updateTimestamp()

          if (decrypted == decrypt(decrypt(decrypt(encrypted)))) {
            client.passwordsDecrypted += 1
            sender ! PasswordCorrect(decrypted)
          }
          else {
            client.passwordsInvalid += 1
            sender ! PasswordIncorrect(decrypted)
          }
      }

    case SendMeStatistics =>
      clients = clients filter { case (t, c) => c.isActive }
      sender ! Statistics(clients.values.toSeq)
  }
}