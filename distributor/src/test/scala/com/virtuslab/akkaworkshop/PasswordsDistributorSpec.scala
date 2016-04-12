package com.virtuslab.akkaworkshop

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.apache.commons.codec.binary.Base64
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class PasswordsDistributorSpec(_system: ActorSystem)
  extends TestKit(_system)
  with ImplicitSender
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("PasswordsDistributorSpec"))

  override def afterAll(): Unit = {
    val termination = system.terminate()
    Await.result(termination, 10.seconds)
  }

  "An PasswordsDistributor" should "be able to provide encoded passwords for register clients and validate decoded ones" in {
    import com.virtuslab.akkaworkshop.PasswordsDistributor._

    val distributor = TestActorRef(PasswordsDistributor.props)
    distributor ! Register("kuki")
    val token = expectMsgType[Registered].token
    token should not be empty

    distributor ! SendMeEncryptedPassword(token)
    val encryptedPassword = expectMsgType[EncryptedPassword].encryptedPassword
    encryptedPassword should not be empty

    def decrypt(p: String) = new String(Base64.decodeBase64(p.getBytes))
    val decryptedPassword = decrypt(decrypt(decrypt(encryptedPassword)))
    distributor ! ValidateDecodedPassword(token, encryptedPassword, decryptedPassword)
    expectMsgType[PasswordCorrect]

    distributor ! ValidateDecodedPassword(token, encryptedPassword, encryptedPassword)
    expectMsgType[PasswordIncorrect]

    distributor ! SendMeStatistics
    val client = expectMsgType[Statistics].clients.head
    client.passwordsRequested should be (1)
    client.passwordsDecrypted should be (1)
    client.passwordsInvalid should be (1)
  }
}
