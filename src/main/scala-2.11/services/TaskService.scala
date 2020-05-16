package services

import akka.actor.{Actor, ActorLogging, ActorRef}
import packet.PacketMSG
import base.Cmd._
import mechanics.{GameService, Channel}

object TaskService {
  // ----- API -----
  case class CommandTask(session: ActorRef, comm: PacketMSG)
}

class TaskService extends Actor with ActorLogging {
  import TaskService._

  def receive = {
    case task: CommandTask => handlePacket(task)
    case _ => log.info("unknown message")
  }

  // ----- actors
  val authService = context.actorSelection("akka://system/user/AuthService")
  val gameService = context.actorSelection("akka://system/user/GameService")

  // ----- actions -----
  def handlePacket(task: CommandTask) = {
    task.comm.cmd match {
      case CmdLogin.code => authService ! AuthService.Authenticate(task.session, task.comm)
      case CmdSetFamily.code => gameService ! GameService.SetAccountFamily(task.session, task.comm)
      case CmdSetNewAvatar.code => gameService ! GameService.SetNewAvatar(task.session, task.comm)
      case CmdJoinToChannel.code => gameService ! GameService.JoinGame(task.session, task.comm)
      case CmdDisconnect.code => gameService ! GameService.LeftGame(task.session)
        ////
      case CmdAvatarMove.code => gameService ! Channel.AvatarMove(task.session, task.comm)
      case CmdAvatarAnimChange.code => gameService ! Channel.AvatarAnimChange(task.session, task.comm)
      case CmdShoot.code => gameService ! Channel.AvatarShoot(task.session, task.comm)
      case CmdShootToAvatar.code => gameService ! Channel.AvatarShootToAvatar(task.session, task.comm)
        ////
      case _ => // illegal, ban him?
    }
  }


  //////////////////////////////////
  override def preStart() {
    log.info("Started TaskService")
  }
  override def postStop() {
    log.info("Stopped TaskService")
  }
}
