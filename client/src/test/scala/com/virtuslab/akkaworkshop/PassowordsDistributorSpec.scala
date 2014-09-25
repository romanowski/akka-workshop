package com.virtuslab.akkaworkshop

import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }
import akka.actor.{ Actor, Props, ActorSystem }
import akka.testkit.{ ImplicitSender, TestKit, TestActorRef }
import scala.concurrent.duration._
import org.apache.commons.codec.binary.Base64

class PassowordsDistributorSpec(_system: ActorSystem)
  extends TestKit(_system)
  with ImplicitSender
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("PassowordsDistributorSpec"))

  override def afterAll: Unit = {
    system.shutdown()
    system.awaitTermination(10.seconds)
  }

  "An PasswordsDistributor" should "be able to provide encoded passwords for register clients and validate decoded ones" in {
    import PasswordsDistributor._

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
