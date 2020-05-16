import akka.actor.{ActorSystem, Props}
import frontend.AkkaIoTcpServer
import mechanics.GameService
import services.{AuthService, DBService, TaskService}

class Server(ip:String, port:Int) {
  // create the actor system and actors
  val actorSystem = ActorSystem("system")

  val actorTaskService =      actorSystem.actorOf(Props[TaskService],"TaskService")
  val actorTcpServer =        actorSystem.actorOf(AkkaIoTcpServer.props(ip,port),"TcpServer")
  val actorAuthService =      actorSystem.actorOf(Props[AuthService],"AuthService")
  val actorDBService =        actorSystem.actorOf(Props[DBService],"DBService")
  val actorGameService =      actorSystem.actorOf(Props[GameService],"GameService")
}