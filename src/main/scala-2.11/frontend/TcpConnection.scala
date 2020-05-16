package frontend

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import akka.io.Tcp
import akka.io.Tcp._
import akka.util.ByteString
import base.Cmd._
import base.Value
import packet.PacketMSG
import services.TaskService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object TcpConnection {
  def props(id: Long, connection: ActorRef) = Props(new TcpConnection(id, connection))
  // ----- heartbeat ----- Checking client connection for life
  case object Heartbeat
  val period = 10 seconds
  // ----- API -----
  case class Send(cmd: Value, data: Array[Byte])
  case class SendBanned(cmd: Value, banTime: Int, banDuration: Int)
  case object CloseConnection
}


class TcpConnection(val id: Long, connection: ActorRef) extends Actor with ActorLogging {
  import TcpConnection._

  def receive = {
    case Send(cmd, data) => sendData(cmd, data)
    case Heartbeat => sendHeartbeat()
    case Received(data) => receiveData(data)
    case CloseConnection => context stop self
    case PeerClosed => context stop self
    case _: Tcp.ConnectionClosed => context stop self
    case _ => log.info("unknown message")
  }

  private var scheduler: Cancellable = _

  // ----- actors
  val taskService = context.actorSelection("akka://system/user/TaskService")

  // ----- actions -----
  def receiveData(data: ByteString) {
    //log.info(s"data from client: ${data.utf8String}")
    val packet: PacketMSG = PacketMSG.parseFrom(data.toArray)
    //log.info(s"Received data: {${packet.toString}}")
    taskService ! TaskService.CommandTask(self, packet)
  }

  def sendData(cmd: Value, data: Array[Byte]) = {
    val packet: PacketMSG = PacketMSG().withCmd(cmd.code).withData(com.google.protobuf.ByteString.copyFrom(data))
    val bytes: ByteString = ByteString(packet.toByteArray)

    connection ! Write(bytes)
  }

  def sendHeartbeat(): Unit = {
    sendData(CmdPing, Array[Byte]())
  }


  //////////////////////////////////
  override def preStart() {
    scheduler = context.system.scheduler.schedule(period, period, self, Heartbeat)
    log.info("Session start: {}", toString)
  }
}

