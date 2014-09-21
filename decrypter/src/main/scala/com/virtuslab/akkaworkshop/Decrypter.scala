package com.virtuslab.akkaworkshop

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.IllegalStateException
import org.apache.commons.codec.binary.Base64
import scala.util.Random

sealed trait DecryptionState
case class PasswordEncrypted(password: String) extends DecryptionState
case class PasswordPrepared(password: String) extends DecryptionState
case class PasswordDecoded(password: String) extends DecryptionState
case class PasswordDecrypted(password: String) extends DecryptionState

object Decrypter {
  private val maxClientsCount = 4

  private var clientsCount = 0

  private val isInitialized = lockInstance()

  private var clients = scala.collection.immutable.ListSet[Int]()

  private var currentId = 0

  private def getNewId(): Int = {
    this synchronized {
      val id = currentId
      clients = clients + id
      currentId += 1
      id
    }
  }

  private def lockInstance(): Boolean = {
    try {
      val tempDir = System.getProperty("java.io.tmpdir")
      val file = new File(s"${tempDir}/akka_workshop")
      val randomAccessFile = new RandomAccessFile(file, "rw")
      val fileLock = randomAccessFile.getChannel().tryLock()

      if (fileLock != null) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
          override def run() {
            try {
              fileLock.release()
              randomAccessFile.close()
              file.delete()
            } catch { case _: Throwable => }
          }
        })
        return true
        println("true")
      }
    } catch { case _: Throwable => }
    return false
  }

  private def decrypt(id: Int, password: String, probabilityOfFailure: Double = 0.05) = {
    def isClientAccepted() = this synchronized {
      if (clientsCount < maxClientsCount) {
        clientsCount += 1
        true
      }
      else {
        false
      }
    }

    try {
      if (!isInitialized) {
        throw new IllegalStateException("Initialization failed. Only single jvm instance is supported.")
      }

      Thread.sleep(1000)

      while (!isClientAccepted()) { Thread.sleep(100) }

      this synchronized {
        val shouldFail = Random.nextInt.abs < probabilityOfFailure * Int.MaxValue.toDouble
        if (shouldFail) {
          clients = clients.empty
          throw new IllegalStateException("Invalid internal state!")
        }
        if (clients.contains(id)) new String(Base64.decodeBase64(password.getBytes)) else "-fj;^)%:-((oh@6#gH%dF6Ljk6%5"
      }

    }
    finally {
      this synchronized {
        clientsCount -=1
      }
    }
  }
}

class Decrypter {
  val id = Decrypter.getNewId()

  def prepare(state: PasswordEncrypted): PasswordPrepared = PasswordPrepared(Decrypter.decrypt(id, state.password))

  def decode(state: PasswordPrepared): PasswordDecoded = PasswordDecoded(Decrypter.decrypt(id, state.password))

  def decrypt(state: PasswordDecoded): PasswordDecrypted = PasswordDecrypted(Decrypter.decrypt(id, state.password))
}