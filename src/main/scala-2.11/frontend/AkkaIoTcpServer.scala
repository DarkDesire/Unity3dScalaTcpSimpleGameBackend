package frontend

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}

object AkkaIoTcpServer {
  def props(address: String, port:Int) = Props(new AkkaIoTcpServer(address,port))
}


class AkkaIoTcpServer(address: String, port:Int) extends Actor with ActorLogging{
  import context.system

  var connectionIds = 0

  def receive = {
    case b @ Bound(localAddress) => // do some logging or setup ...

    case CommandFailed(_: Bind) => context stop self

    case c @ Connected(remote, local) =>
      val connection = sender()
      connectionIds += 1
      val handler = context.actorOf(TcpConnection.props(connectionIds,connection))
      connection ! Register(handler)
  }

  IO(Tcp) ! Bind(self, new InetSocketAddress(address, port))


  //////////////////////////////////
  override def preStart(){
    log.info(s"TcpServer on ${address}:${port} started")
  }
}

