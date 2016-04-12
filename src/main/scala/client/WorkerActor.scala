package client

import akka.actor.{OneForOneStrategy, SupervisorStrategy, Actor, ActorRef}
import com.virtuslab.akkaworkshop._


case class WorkerRequest(from: ActorRef, originalPassword: String)

case class WorkerResult(originalPassword: String, decrypted: String)

class WorkerActor extends Actor{

  private val decrypter = new Decrypter

  def justRetry(receive: Receive): Receive = new PartialFunction[Any, Unit] {
    override def isDefinedAt(x: Any): Boolean = receive.isDefinedAt(x)

    override def apply(v1: Any): Unit = try{
      receive.apply(v1)
    } catch {
      case missingState: IllegalStateException =>
        self ! v1
        throw missingState
    }
  }

  override def receive: Receive = justRetry {
    case req: WorkerRequest =>
        self ! (req, decrypter.prepare(req.originalPassword))

    case (req: WorkerRequest, prepared: PasswordPrepared) =>
      self ! (req, decrypter.decode(prepared))

    case (req: WorkerRequest, decoded: PasswordDecoded) =>
      req.from ! WorkerResult(req.originalPassword, decrypter.decrypt(decoded))
  }
}
