package com.virtuslab.akkaworkshop

import org.scalatest._
import org.scalatest.concurrent.Eventually

class DecrypterSpec extends FlatSpec with Matchers with Eventually {

  "Decrypter" should "be able to decrypt password" in {
    val d = new Decrypter()
    d.decrypt(d.decode(d.prepare("U1hwT2FWRjZTbnBhUkhONlRrRTlQUT09"))) shouldEqual "#3bC2sd;34"
  }
}
