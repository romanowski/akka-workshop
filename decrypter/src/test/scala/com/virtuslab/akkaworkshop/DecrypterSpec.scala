package com.virtuslab.akkaworkshop

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.concurrent.Eventually

class DecrypterSpec extends FlatSpec with ShouldMatchers with Eventually {
  "Decrypter" should "be able to decrypt password" in {
    val d = new Decrypter()
    d.decrypt(d.decode(d.prepare(PasswordEncrypted("U1hwT2FWRjZTbnBhUkhONlRrRTlQUT09")))).password should be ("#3bC2sd;34")
  }
}
