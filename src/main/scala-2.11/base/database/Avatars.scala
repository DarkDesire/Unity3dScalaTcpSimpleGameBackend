package base.database

import org.joda.time.Instant
import slick.driver.PostgresDriver.api._

case class AvatarDAO(var familyId:Int,
                     var name:String,
                     var level:Int,
                     var transform: Option[String] = None,
                     var deathDescriptor: Option[String] = None,
                     var healthDescriptor: Option[String] = None,
                     var mannaDescriptor: Option[String] = None,
                     var lastOnline:Option[Long] = Some(Instant.now().getMillis()),
                     id:Option[Int] = None)

// A Avatars table with 9 columns: familyId, name, level, transform, deathDescriptor, healthDescriptor, mannaDescriptor, lastOnline, id
class Avatars(tag: Tag) extends Table[AvatarDAO](tag, "AVATARS") {

  def id = column[Int]("ID", O.AutoInc, O.PrimaryKey)
  def familyId = column[Int]("FAMILY_ID")
  def name = column[String]("NAME")
  def level = column[Int]("LEVEL")
  def transform = column[Option[String]]("TRANSFORM")                    // TODO: change type to json
  def deathDescriptor = column[Option[String]]("DEATH_DESCRIPTOR")       // TODO: change type to json
  def healthDescriptor = column[Option[String]]("HEALTH_DESCRIPTOR")     // TODO: change type to json
  def mannaDescriptor = column[Option[String]]("MANNA_DESCRIPTOR")       // TODO: change type to json
  def lastOnline = column[Option[Long]]("LAST_ONLINE")                   // TODO: change type to joda time

  def * = (familyId, name, level, transform, deathDescriptor, healthDescriptor, mannaDescriptor, lastOnline, id.?) <> (AvatarDAO.tupled,AvatarDAO.unapply)

  def family = foreignKey("FAMILY_FK", familyId, TableQuery[Families])(_.id) // TODO: add more restrict on update, on delete
}
