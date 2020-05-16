package mechanics

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import base.Cmd._
import base._
import frontend.TcpConnection
import org.joda.time.{DateTime, Instant}
import packet.{PacketAvatarMove, _}
import services.DBService

import scala.concurrent.duration._
import scala.collection.mutable

object Channel {
  // ----- game tick -----
  private val gameTick = 100.millisecond
  // ----- save avatar tick -----
  private val saveAvatarTick = 1 second
  // ----- respawn time -----
  private val respawnTime = 10 second

  // ----- API -----
  case object TickGame
  case object TickSaveAvatars

  case class JoinChannel(session: ActorRef, avatar: Avatar)
  case class LeftChannel(session: ActorRef)
  case class LeftChannelFinished(session: ActorRef, familyId: Int)
  case class AvatarMove(session: ActorRef, comm: PacketMSG)
  case class AvatarAnimChange(session: ActorRef, comm: PacketMSG)
  case class AvatarShoot(session: ActorRef, comm: PacketMSG)
  case class AvatarShootToAvatar(session: ActorRef, comm: PacketMSG)
  case class Respawn(session: ActorRef)

  // safe constructor
  def props(id: Int, gameService: ActorRef) = Props(new Channel(id, gameService))
}


class Channel(channelId: Int, gameService: ActorRef) extends Actor with ActorLogging {

  import Channel._
  import context._

  override def receive = {
    case TickGame => channelTick
    case TickSaveAvatars => handleTickSaveAvatars
    case task: JoinChannel => handleJoinChannel(task)
    case task: LeftChannel => handleLeftChannel(task)
    case task: AvatarMove => handleMove(task)
    case task: AvatarAnimChange => handleAnimChange(task)
    case task: AvatarShoot => handleShoot(task)
    case task: AvatarShootToAvatar => handleShootToAvatar(task)
    case task: Respawn => handleRespawn(task)

    case _ => log.info("unknown message")
  }

  val avatars: mutable.HashMap[ActorRef, Avatar] = mutable.HashMap.empty[ActorRef, Avatar]

  // ----- actors
  val dbService = context.actorSelection("akka://system/user/DBService")

  // ----- actions -----
  def makePacketAvatar(avatar: Avatar): PacketAvatar = {
    val packetPosition: PacketPosition = new PacketPosition().withX(avatar.transform.position.x).withY(avatar.transform.position.y).withZ(avatar.transform.position.z)
    val packetRotation: PacketRotation = new PacketRotation().withPitch(avatar.transform.rotation.pitch).withYaw(avatar.transform.rotation.yaw)
    val packetTransform: PacketTransform = new PacketTransform().withPosition(packetPosition).withRotation(packetRotation)
    val packetHealth = new PacketHealth().withCurrent(avatar.health.current).withMaximum(avatar.health.maximum)
    val packetManna = new PacketManna().withCurrent(avatar.manna.current).withMaximum(avatar.manna.maximum)
    val packetAvatar = new PacketAvatar().withAvatarId(avatar.id).withTransform(packetTransform).withAnimState(avatar.animState).withHealth(packetHealth).withManna(packetManna)
    packetAvatar
  }

  def makeExtendedPacketAvatar(avatar: Avatar): PacketAvatar = {
    val packetAvatar = makePacketAvatar(avatar)
    val resultPacketAvatar = packetAvatar.withFamilyId(avatar.familyId).withFamilyName(avatar.familyName).withName(avatar.name).withLevel(avatar.level)
    resultPacketAvatar
  }

  def channelTick = {

    val deadAvatars = avatars.values.filter( avatar => avatar.IsDead ).foreach (avatar => {
      // check if deathLastTime set
      if (!avatar.isWaitingRespawn) {
        avatar.WaitRespawn(true)
        val avatarRef = avatars.find( pair => pair._2.id == avatar.id).get._1
        context.system.scheduler.scheduleOnce(respawnTime,self,Respawn(avatarRef))
      }
      avatar.changes(isChanged = true, isSavedInDB = false)
    })

    val updatedAvatars = avatars.values.filter(avatar => avatar.isChanged).map(avatar => {
      makePacketAvatar(avatar)
    }).toSeq

    if (updatedAvatars.nonEmpty){
      val packetAvatars = new PacketAvatars().withAvatars(updatedAvatars)
      broadcast(CmdAvatars, packetAvatars.toByteArray)
    }
  }

  def handleTickSaveAvatars: Unit = {
    val avatrs = avatars.clone()
    val avatarsForSaving = avatrs.values.filter( avatar => !avatar.isSavedInDB).to[List]
    if (avatarsForSaving.nonEmpty) {
      // check if we have avatars for saving
      dbService ! DBService.UpdateAvatars(avatarsForSaving)
      avatarsForSaving.foreach(avatar =>
        avatar.changes(isChanged = false, isSavedInDB = true)
      )
    }
  }

  def handleJoinChannel(task: JoinChannel) = {
    val localAvatar = task.avatar
    avatars.put(task.session, localAvatar)
    // send to all avatars a new one
    val packetAvatar = makeExtendedPacketAvatar(localAvatar)
    val packetJoinedAvatar = new PacketJoinedAvatar().withAvatar(packetAvatar)
    broadcast(CmdJoinedAvatar, packetJoinedAvatar.toByteArray)

    // send to new avatar already joined avatars
    val others = avatars.values.filter(avatar => avatar.id != localAvatar.id)
    others.foreach(
      avatar => {
        val packetAvatar = makeExtendedPacketAvatar(avatar)
        val packetJoinedAvatar = new PacketJoinedAvatar().withAvatar(packetAvatar)
        task.session ! TcpConnection.Send(CmdJoinedAvatar, packetJoinedAvatar.toByteArray)
      }
    )
  }

  def handleLeftChannel(task: LeftChannel) = {
    val avatar = avatars(task.session)

    // send to all AvatarLeft with avatarId
    val packetAvatarLeft = new PacketAvatarLeft().withAvatarId(avatar.id)
    broadcast(CmdAvatarLeft, packetAvatarLeft.toByteArray)

    sender ! Channel.LeftChannelFinished(task.session, avatar.familyId)

    avatars.remove(task.session)
  }

  def handleMove(task: AvatarMove) = {
    val packetAvatarMove = PacketAvatarMove.parseFrom(task.comm.data.toByteArray)
    val receivedAvatar = packetAvatarMove.avatar.head

    val activeAvatar = avatars.values.find(avatar => avatar.id == receivedAvatar.avatarId).head

    val avatarTransform = Transform.fromPacketTransform(receivedAvatar.transform.head)
    if (activeAvatar.transform != avatarTransform) {
      activeAvatar.transform.fromTransform(avatarTransform)
      activeAvatar.changes(isChanged = true, isSavedInDB = false)
    }
  }

  def handleAnimChange(task: AvatarAnimChange) = {
    val packetAnimChange = PacketAvatarAnimChange.parseFrom(task.comm.data.toByteArray)
    val receivedAvatar = packetAnimChange.avatar.head

    val activeAvatar = avatars.values.find(avatar => avatar.id == receivedAvatar.avatarId).head

    if (activeAvatar.animState != receivedAvatar.animState) {
      activeAvatar.animState = receivedAvatar.animState
      activeAvatar.changes(isChanged = true, isSavedInDB = false)
    }
  }

  def handleShoot(task: AvatarShoot) = {
    val packetShoot = PacketShoot.parseFrom(task.comm.data.toByteArray)
    val packetPosition = packetShoot.shootEndPosition.head

    // TODO: check for cooldown, distance
    val packetShootSuccess = new PacketShootSuccess().withSenderAvatar(makePacketAvatar(avatars(task.session))).withShootEndPosition(packetPosition)
    broadcast(CmdShootSuccess, packetShootSuccess.toByteArray)
  }

  def handleShootToAvatar(task: AvatarShootToAvatar) = {
    val packetShootToAvatar = PacketShootToAvatar.parseFrom(task.comm.data.toByteArray)
    val receivedAvatarId = packetShootToAvatar.avatarId

    val killerAvatar = avatars(task.session)
    // TODO: add check for cooldown, distance

    val victimAvatar = avatars.values.find(avatar => avatar.id == receivedAvatarId).head

    if (victimAvatar.health.IsAlive) { // prevent second check
      victimAvatar.health.changeHealth(-Avatar.damage)

      // send to all Avatars ShootToAvatarSuccess
      val packetShootToAvatarSuccess = new PacketShootToAvatarSuccess().withSenderShootAvatarId(killerAvatar.id).withReceiverShootAvatarId(victimAvatar.id)
      broadcast(CmdShootToAvatarSuccess, packetShootToAvatarSuccess.toByteArray)
      // check if victim is dead
      if (victimAvatar.health.IsDead) {
        val packetKilledBy = new PacketKilledBy().withKillerAvatarId(killerAvatar.id).withVictimAvatarId(victimAvatar.id)
        broadcast(CmdKilledBy, packetKilledBy.toByteArray)
        victimAvatar.isWaitingRespawn = true
        val victimAvatarRef = avatars.find( pair => pair._2.id == victimAvatar.id).get._1
        context.system.scheduler.scheduleOnce(respawnTime,self,Respawn(victimAvatarRef))
      }
      victimAvatar.changes(isChanged = true, isSavedInDB = false)
    }

  }

  def handleRespawn(task: Respawn) = {
    if (avatars.contains(task.session)){ //if player still online
      val avatar = avatars(task.session)
      avatar.reset

      val packetRespawn = new PacketRespawn().withAvatar(makePacketAvatar(avatar))
      broadcast(CmdRespawn,packetRespawn.toByteArray)
    }
  }

  def broadcast(cmd: Value, data: Array[Byte]) = {
    avatars.keySet.foreach(session => session ! TcpConnection.Send(cmd, data))
  }

  //////////////////////////////////
  override def preStart() {
    log.info("Started channel: " + channelId)
    context.system.scheduler.schedule(gameTick, gameTick, self, TickGame)
    context.system.scheduler.schedule(saveAvatarTick, saveAvatarTick, self, TickSaveAvatars)
  }

  override def postStop() {
    log.info("Stopped channel: " + channelId)
  }
}
