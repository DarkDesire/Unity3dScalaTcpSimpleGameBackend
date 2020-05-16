package services

import akka.actor.{Actor, ActorLogging, ActorRef}
import base.Access
import base.Cmd.{CmdLoginFailed, CmdLoginSuccess}
import base.database.Account
import frontend.TcpConnection
import mechanics.GameService
import packet.PacketMSG

import scala.collection.mutable

object AuthService {
  // ----- API -----
  case class Authenticate(session: ActorRef, comm: PacketMSG)
  case class AuthenticateSuccess(session: ActorRef, account: Account)
  case class AuthenticateFailed(session: ActorRef)
}


class AuthService extends Actor with ActorLogging {
  import AuthService._

  override def receive = {
      // auth
      case task: Authenticate  => handleAuth(task)
      case task: AuthenticateSuccess => handleAuthSuccess(task)
      case task: AuthenticateFailed => handleAuthFailed(task)
      case _ => log.info("unknown message")
  }

  // -- local vals
  val accounts: mutable.HashMap[ActorRef, Account] = mutable.HashMap.empty[ActorRef, Account]

  // ----- actors
  val taskService = context.actorSelection("akka://system/user/TaskService")
  val dbService = context.actorSelection("akka://system/user/DBService")
  val gameService = context.actorSelection("akka://system/user/GameService")

  // ----- actions -----
  def handleAuth(task: Authenticate) = {
    if (accounts.contains(task.session)) { // account already logged in
      // let's disconnect it?
    }  else {
      dbService ! DBService.GetAccount(task.session, task.comm)
    }
  }

  def handleAuthSuccess(task: AuthenticateSuccess) = {
    accounts.put(task.session, task.account)
    task.session ! TcpConnection.Send(CmdLoginSuccess, Array[Byte]())

    if (isAccountHaveAccess(task.account)) {
      gameService ! GameService.AccountAuthenticatedSuccess(task.session, task.account.id.get, task.account.familyId)
    } else {
      println("You haven't access buy a starter pack. Buy it on web site!")
    }
  }

  def handleAuthFailed(task: AuthenticateFailed) = {
    task.session ! TcpConnection.Send(CmdLoginFailed, Array[Byte]())
  }

  private def isAccountHaveAccess(account: Account):Boolean = {
    account.accessId match {
      case Some(Access.Traveler.code) =>
        println(s"You have a traveler pack. You can play right now!")
        true
      case Some(Access.Explorer.code) =>
        println(s"You have a explorer pack. You can play right now!")
        true
      case Some(Access.Conqueror.code) =>
        println(s"You have a conqueror pack. You can play right now!")
        true
      case other =>
        println(s"Smth went wrong. Access id:$other, accountId:${account.id.get}")
        false
    }
  }

  //////////////////////////////////
  override def preStart() {
    log.info("Started AuthService")
  }
  override def postStop() {
    log.info("Stopped AuthService")
  }
}
