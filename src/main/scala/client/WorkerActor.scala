package client

import akka.actor._
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


  override def supervisorStrategy: SupervisorStrategy = AllForOneStrategy()(SupervisorStrategy.defaultDecider)

  override def receive: Receive = fineDecrypter

  def fineDecrypter =  justRetry {
    case req: WorkerRequest =>
        self ! (req, decrypter.prepare(req.originalPassword))

    case (req: WorkerRequest, prepared: PasswordPrepared) =>
      self ! (req, prepared, decrypter.decode(prepared))

    case (req: WorkerRequest, prepared, decoded: PasswordDecoded) =>
      self ! (req, prepared, decoded, decrypter.decrypt(decoded))

    case (req: WorkerRequest, _, _, decrypted: String) =>
      req.from ! WorkerResult(req.originalPassword, decrypted)

    case other =>
      println(s"Missed in normal: $other")
  }

  def brokenDecrypter =  justRetry {
    case req: WorkerRequest =>
      context.become(fineDecrypter)
      self ! req

    case (req: WorkerRequest, _) =>
      context.become(fineDecrypter)
      self ! req

    case (req: WorkerRequest, prepared, _) =>
      context.become(fineDecrypter)
      self ! (req, prepared)

    case (req: WorkerRequest, prepared, decoded, _) =>
      context.become(fineDecrypter)
      req.from ! (req, prepared, decoded)

    case other =>
      println(s"Missed in new: $other")
  }

  @throws[Exception](classOf[Exception])
  override def postRestart(reason: Throwable): Unit = {
    super.postRestart(reason)
    context.become(brokenDecrypter)
  }
}
