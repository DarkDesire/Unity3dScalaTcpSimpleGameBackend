package services

import akka.actor.{Actor, ActorLogging, ActorRef}
import base._
import base.database._
import mechanics.GameService
import packet.{PacketLogin, PacketMSG, PacketSetFamily, PacketSetNewAvatar}
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


object DBService {
  // ----- API -----
  case class GetAccount(session: ActorRef, comm: PacketMSG)
  case class UpdateAccountFamily(session: ActorRef, comm: PacketMSG, accountId: Int)
  case class GetAllFamilyAvatars(session: ActorRef, familyId: Int)
  case class AddNewAvatar(session: ActorRef, comm: PacketMSG, familyId: Int, startLevel: Int,  maxHealth:Int, maxManna:Int)
  case class UpdateAvatars(avatars: List[Avatar])
}


class DBService extends Actor with ActorLogging {
  import DBService._

  override def receive = {
    case task: GetAccount => returnAccount(task, sender)
    case task: GetAllFamilyAvatars => returnAvatarsForAccount(task, sender)
    case task: UpdateAccountFamily => updateAccountFamily(task, sender)
    case task: AddNewAvatar => addNewAvatar(task, sender)
    case task: UpdateAvatars => updateAvatars(task)
    case _ => log.info("unknown message")
  }

  val db = Database.forConfig("db")
  // The query interface for the Accesses table
  val accesses: TableQuery[Accesses] = TableQuery[Accesses]
  // the query interface for the Accounts table
  val accounts: TableQuery[Accounts] = TableQuery[Accounts]
  // the query interface for the Avatars table
  val avatars: TableQuery[Avatars] = TableQuery[Avatars]
  // the query interface for the Families table
  val families: TableQuery[Families] = TableQuery[Families]

  private case class Pair(accountId:Int, familyId:Int)

  // ----- actions -----
  def returnAccount(task:  GetAccount, callbackActor: ActorRef) = {
    // parse task
    val cmd = PacketLogin.parseFrom(task.comm.data.toByteArray)

    val query1 = accounts.filter(a => (a.email===cmd.email) && (a.pass===cmd.pass)).to[List]
    val future1 = db.run(query1.result.map(_.headOption))

    future1.onSuccess { case result =>
      if (result.isEmpty) callbackActor ! AuthService.AuthenticateFailed(task.session)
      else {
        result.get match {
          case account: Account => callbackActor ! AuthService.AuthenticateSuccess(task.session, Account("","",account.familyId,account.accessId,account.id))
          case _ => callbackActor ! AuthService.AuthenticateFailed(task.session)
        }
      }
    }
    future1.onFailure { case failure => println(s"Failure: $failure") }
  }

  def updateAccountFamily(task:  UpdateAccountFamily, callbackActor: ActorRef) = {
    // parse task
    val cmd = PacketSetFamily.parseFrom(task.comm.data.toByteArray)
    val newFamilyName = cmd.family

    val future =
      checkFamilyExistByName(newFamilyName)
      .flatMap { bRecordExist =>
        if (!bRecordExist) { // if name doesn't exist
          insertFamilyWithNewFamilyName(newFamilyName)
          .flatMap{ newFamilyId =>
            updateAccountWithIdByFamilyId(task.accountId,newFamilyId)
              .flatMap{ _ => // return pair (accountId, familyId)
                Future(Pair(task.accountId, newFamilyId))
          }}
        } else { // if name exists
          Future (s"Record with $newFamilyName already exists")
        }
      }

    future.onComplete( result =>  {
      println(s"Result: ${result}")
      result.get match {
        case pair: Pair => {
          println(s"Success upadated account with id:${pair.accountId}. New family id:${pair.familyId}.")
          callbackActor ! GameService.SetAccountFamilySuccess(task.session, pair.accountId, Some(pair.familyId))
        }
        case failureMsg: String => {
          println(s"Failure: ${failureMsg}")
          callbackActor ! GameService.SetAccountFamilyFailed(task.session, failureMsg)
        }
       }
    })
  }

  def returnAvatarsForAccount(task: GetAllFamilyAvatars, callbackActor: ActorRef) = {
    val familyId = task.familyId
    returnFamilyById(familyId).onComplete( family =>
      returnAvatarsByFamilyId(familyId).onComplete { result =>
        val avatars = result.get.map(avatarDAO => {
          Avatar(avatarDAO,family.get.name)
        })
        callbackActor ! GameService.AllFamilyAvatars(task.session,familyId,avatars)
      }
    )
  }

  def updateAvatars(task: UpdateAvatars) = {
    val toBeInserted = task.avatars.map { avatar => avatars.insertOrUpdate(AvatarDAO(avatar.familyId,avatar.name,avatar.level,Some(avatar.transform.returnJsonString),None,Some(avatar.health.returnJsonString),Some(avatar.manna.returnJsonString), None, Some(avatar.id)))}
    val inOneGo = DBIO.sequence(toBeInserted)
    val dbioFuture = db.run(inOneGo.transactionally)
  }

  def addNewAvatar(task:  AddNewAvatar, callbackActor: ActorRef) = {
    // parse task
    val cmd = PacketSetNewAvatar.parseFrom(task.comm.data.toByteArray)
    val newAvatarName = cmd.name

    val future =
      checkAvatarExistByName(newAvatarName)
        .flatMap { bRecordExist =>
          if (!bRecordExist) { // if name doesn't exist
            insertAvatarWithNewAvatarNameAndFamilyId(newAvatarName,task.familyId,task.startLevel, task.maxHealth, task.maxManna)
              .flatMap{ newAvatarId =>
                  returnAvatarById(newAvatarId)
                }
          } else { // if name exists
            Future (s"Record with $newAvatarName already exists.")
          }
        }

    future.onComplete( result =>  {
      println(s"Result: ${result}")
      result.get match {
        case avatar: AvatarDAO => {
          println(s"Success: ${avatar}")
          callbackActor ! GameService.SetNewAvatarSuccess(task.session)
        }
        case failureMsg: String => {
          println(s"Failure: ${failureMsg}")
          callbackActor ! GameService.SetNewAvatarFailed(task.session, failureMsg)
        }
      }
    })
  }

  // check if avatar with avatarName exists
  def checkAvatarExistByName(avatarName: String): Future[Boolean] =
    db.run(
      avatars.filter(_.name === avatarName).exists.result
    )
  
  // insert avatar with newAvatarName, familyId and return it's id
  def insertAvatarWithNewAvatarNameAndFamilyId(avatarName: String, familyId:Int, startLevel:Int, maxHealth:Int, maxManna:Int): Future[Int] =
    db.run(
      avatars returning avatars.map(_.id) += AvatarDAO(familyId, avatarName, startLevel, Some(Transform.default.returnJsonString),None, Some(Health.default.returnJsonString),Some(Manna.default.returnJsonString),None)
    )
  
  def returnAvatarsByFamilyId(familyId: Int): Future[List[AvatarDAO]] =
    db.run(
      avatars.filter(_.familyId === familyId).to[List].result
    )

  def returnAvatarById(avatarId: Int): Future[AvatarDAO] =
    db.run(
      avatars.filter(_.id === avatarId).result.head
    )

  def returnAccountById(accountId: Int): Future[Account] =
    db.run(
      accounts.filter(_.id === accountId).to[List].result.head
    )

  // update familyId with newFamId in account with accountId
  def updateAccountWithIdByFamilyId(accountId: Int, newFamilyId: Int): Future[Int] =
    db.run(
      accounts.filter(_.id === accountId).map(_.familyId).update(Some(newFamilyId))
    )

  // check if family with familyName exists
  def checkFamilyExistByName(familyName: String): Future[Boolean] =
    db.run(
      families.filter(_.name === familyName).exists.result
    )

  def returnFamilyById(familyId: Int): Future[Family] =
    db.run(
      families.filter(_.id === familyId).to[List].result.head
    )

  // insert family with newFamilyName and return it's id
  def insertFamilyWithNewFamilyName(familyName: String): Future[Int] =
    db.run(
      families returning families.map(_.id) += Family(familyName)
    )


  //////////////////////////////////
  override def preStart() {
    log.info("Started DBService")
  }
  override def postStop() {
    log.info("Stopped DBService")
  }
}
