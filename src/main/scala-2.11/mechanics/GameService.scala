package mechanics

import akka.actor.{Actor, ActorLogging, ActorRef}
import base.Avatar
import base.Cmd._
import com.typesafe.config.ConfigFactory
import frontend.TcpConnection
import mechanics.Channel.{AvatarMove, LeftChannelFinished}
import packet._
import services.DBService

import scala.collection.mutable

object GameService {
  // ----- API -----
  // 1
  case class AccountAuthenticatedSuccess(session: ActorRef, accountId:Int, familyId:Option[Int])

  case class AllFamilyAvatars(session: ActorRef, familyId: Int, avatars: List[Avatar])

  case class SetAccountFamily(session: ActorRef, comm: PacketMSG)
  case class SetAccountFamilySuccess(session: ActorRef, accountId:Int, familyId:Option[Int])
  case class SetAccountFamilyFailed(session: ActorRef, reason: String)

  case class SetNewAvatar(session: ActorRef, comm: PacketMSG)
  case class SetNewAvatarSuccess(session: ActorRef)
  case class SetNewAvatarFailed(session: ActorRef, reason: String)

  case class JoinGame(session: ActorRef, comm: PacketMSG)
  case class LeftGame(session: ActorRef)
}

class GameService extends Actor with ActorLogging {
  import GameService._

  override def receive = {
    // from Auth
    case task: AccountAuthenticatedSuccess => handleAccountAuthenticatedSuccess(task)
    // family
    case task: SetAccountFamily => handleSetFamily(task)
    case task: SetAccountFamilySuccess => handleSetFamilySuccess(task)
    case task: SetAccountFamilyFailed => handleSetFamilyFailed(task)
    // avatar
    case task: AllFamilyAvatars => handleAllFamilyAvatars(task)
    case task: SetNewAvatar => handleSetNewAvatar(task)
    case task: SetNewAvatarSuccess => handleSetNewAvatarSuccess(task)
    case task: SetNewAvatarFailed => handleSetNewAvatarFailed(task)
    // gameplay
    case task: JoinGame => handleJoinGame(task)
    case task: LeftGame => handleLeftGame(task)
    case task: LeftChannelFinished => handleLeftChannelFinished(task)
    case task: Channel.AvatarMove => handleMove(task)
    case task: Channel.AvatarAnimChange => handleAnimChange(task)
    case task: Channel.AvatarShoot => handleShoot(task)
    case task: Channel.AvatarShootToAvatar => handleShootToAvatar(task)

    case task => log.info(s"unknown message ${task}")
  }

  // actor link to (accountId,familyId)
  val accounts: mutable.HashMap[ActorRef, (Int,Option[Int])] = mutable.HashMap.empty[ActorRef, (Int,Option[Int])]
  // familyId to List[Avatar]
  val avatars:  mutable.HashMap[Int, List[Avatar]] = mutable.HashMap.empty[Int, List[Avatar]]

  val channels: mutable.HashMap[Int, ActorRef] = mutable.HashMap.empty[Int, ActorRef]

  // -- config
  val config = ConfigFactory.load().getConfig("server")
  val availableAvatars = config.getInt("AvailableAvatars")
  val maxAvatars = config.getInt("MaxAvatars")
  val startLevel = config.getInt("StartLevel")
  val maxHealth = config.getInt("StartMaxHealth")
  val maxManna = config.getInt("StartMaxManna")

  // local vars
  var channelId = 1

  // ----- actors
  val taskService = context.actorSelection("akka://system/user/TaskService")
  val dbService = context.actorSelection("akka://system/user/DBService")
  val gameService = context.actorSelection("akka://system/user/GameService")

  // ----- actions -----
  def handleAccountAuthenticatedSuccess(task: AccountAuthenticatedSuccess) = {
    accounts.put(task.session,(task.accountId,task.familyId))

    checkAccountHaveFamily(task.session, task.familyId)
  }

  def handleSetFamily(task: SetAccountFamily) = {
    val accountId = accounts(task.session)._1
    dbService ! DBService.UpdateAccountFamily(task.session,task.comm,accountId)
  }

  def handleSetFamilySuccess(task: SetAccountFamilySuccess) = {
    accounts(task.session) = (task.accountId, task.familyId)
    task.session ! TcpConnection.Send(CmdSetFamilySuccess, Array[Byte]())

    checkAccountHaveFamily(task.session, task.familyId)
  }

  def handleSetFamilyFailed(task: SetAccountFamilyFailed) = {
    task.session ! TcpConnection.Send(CmdSetFamilyFailed, Array[Byte]())
  }

  def checkAccountHaveFamily(session: ActorRef, familyId: Option[Int]) = {
    if (isAccountHaveFamily(familyId)) {
      // db service give me all avatars in this account
      dbService ! DBService.GetAllFamilyAvatars(session, familyId.get)
    } else {
      val setFamilyRequest = PacketSetFamilyRequest()
      session ! TcpConnection.Send(CmdSetFamilyRequest, setFamilyRequest.toByteArray)
    }
  }

  def handleAllFamilyAvatars(task: AllFamilyAvatars) = {
    avatars(task.familyId) = task.avatars

    val previewAvatars = task.avatars.map(avatar => makePreviewAvatar(avatar))
    val packetPreviewAvatars = new PacketPreviewAvatars().withAvatars(previewAvatars).withCreatedAvatars(task.avatars.length).withAvailableAvatars(availableAvatars).withMaxAvatars(maxAvatars)

    task.session ! TcpConnection.Send(CmdPreviewAvatars, packetPreviewAvatars.toByteArray)
  }

  def handleSetNewAvatar(task: SetNewAvatar) = {
    val familyId = accounts(task.session)._2.get
    dbService ! DBService.AddNewAvatar(task.session,task.comm, familyId, startLevel, maxHealth, maxManna)
  }

  def handleSetNewAvatarSuccess(task: SetNewAvatarSuccess) = {
    val familyId = accounts(task.session)._2

    task.session ! TcpConnection.Send(CmdSetNewAvatarSuccess, Array[Byte]())

    checkAccountHaveFamily(task.session, familyId)
  }

  def handleSetNewAvatarFailed(task: SetNewAvatarFailed) = {
    task.session ! TcpConnection.Send(CmdSetNewAvatarFailed, Array[Byte]())
  }

  def makePreviewAvatar(avatar: Avatar):PacketPreviewAvatar = {
    new PacketPreviewAvatar().withAvatarId(avatar.id).withName(avatar.name).withLevel(avatar.level)
  }

  private def isAccountHaveFamily(familyId: Option[Int]):Boolean = {
    familyId match  {
      case Some(id) =>
        println(s"You already have a family name. Your family id:${id}")
        true
      case None =>
        println("Set your family name")
        false
    }

  }
  def handleJoinGame(task: JoinGame) = {
    // parse task
    val cmd = PacketJoinToChannel.parseFrom(task.comm.data.toByteArray)
    val avatarId = cmd.avatarId
    val channelId = cmd.channelId

    val familyId = accounts(task.session)._2.get
    val avatar = avatars(familyId).filter(avatar => avatar.id == avatarId).head

    if(channels.isEmpty) {
      createChannel()
    }

    channels(1) ! Channel.JoinChannel(task.session, avatar)
  }

  def handleLeftGame(task: LeftGame) = {
    channels(1) ! Channel.LeftChannel(task.session)
  }

  def handleLeftChannelFinished(task: LeftChannelFinished) = {
    avatars.remove(task.familyId)
    accounts.remove(task.session)
  }

  def createChannel(): ActorRef = {
    val channel = context.actorOf(Channel.props(channelId,self), "channel" + channelId)
    channels.put(channelId, channel)
    channel
  }

  def handleMove(task: Channel.AvatarMove) = {
    channels(1) ! task
  }

  def handleAnimChange(task: Channel.AvatarAnimChange) = {
    channels(1) ! task
  }

  def handleShoot(task: Channel.AvatarShoot) = {
    channels(1) ! task
  }

  def handleShootToAvatar (task: Channel.AvatarShootToAvatar) = {
    channels(1) ! task
  }

  //////////////////////////////////
  override def preStart() {
    log.info("Started GameService")
  }
  override def postStop() {
    log.info("Stopped GameService")
  }
}
