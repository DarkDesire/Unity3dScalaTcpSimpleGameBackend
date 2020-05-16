package base

import base.database.AvatarDAO
import spray.json._
import MyJsonHealthProtocol._
import MyJsonMannaProtocol._
import MyJsonTransformProtocol._
import org.joda.time.DateTime

case class Avatar(id:Int,
                  familyId:Int,
                  familyName:String,
                  name:String,
                  var level:Int,
                  var transform: Transform,
                  var animState: Int,
                  var deathLastTime: Option[Long] = None,
                  var respawnTime: Option[Long] = None,
                  var health: Health,
                  var manna: Manna,
                  var isChanged: Boolean, // pos, rot, health, manna, animstate
                  var isSavedInDB: Boolean,
                  var isWaitingRespawn: Boolean
                 )
{
  override def toString: String = s"PlayerID:${id}. PlayerName:${name}. Transform:${transform}."

  def reset = {
    this.transform = Transform.default
    this.health = Health.default
    this.manna = Manna.default
    this.animState = AnimState.Idle.code
    WaitRespawn(false)
    changes(isChanged = true, isSavedInDB = false)
  }

  def WaitRespawn(value: Boolean) = {
    if (value){
      val currentTime = DateTime.now
      this.deathLastTime = Some(currentTime.getMillis)
      this.respawnTime = Some(currentTime.plusSeconds(10).getMillis)
    } else {
      this.deathLastTime = None
      this.respawnTime = None
    }
    this.isWaitingRespawn = value
  }
  def IsDead = this.health.IsDead
  def IsAlive = !IsDead

  def changes(isChanged: Boolean, isSavedInDB: Boolean) = {
    this.isChanged = isChanged
    this.isSavedInDB = isSavedInDB
  }
}

object Avatar {
  val r = scala.util.Random
  val basicDamage = 30
  val basicCriticalChance = 20
  val maxCriticalChance = 100
  val criticalMultiply = 5

  var value = r.nextInt(basicCriticalChance).toFloat/maxCriticalChance
  var damage = (basicDamage + ((basicDamage*criticalMultiply) * value)).toInt

  def apply(avatarDAO: AvatarDAO, familyName: String) = {
    val health = JsonParser(avatarDAO.healthDescriptor.get).convertTo[Health]
    val manna = JsonParser(avatarDAO.mannaDescriptor.get).convertTo[Manna]
    val transform = JsonParser(avatarDAO.transform.get).convertTo[Transform]
    new Avatar(avatarDAO.id.get, avatarDAO.id.get, familyName, avatarDAO.name, avatarDAO.level, transform, AnimState.Idle.code, None, None, health, manna, false, true, false)
  }
}


