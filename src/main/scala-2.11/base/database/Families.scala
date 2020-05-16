package base.database

import slick.driver.PostgresDriver.api._

case class Family(var name:String, var createdAvatars: Option[Int] = None, var availableAvatars: Option[Int] = None, id: Option[Int] = None)

// A Families table with 4 columns: id, name, createdAvatars, availableAvatars
class Families(tag: Tag) extends Table[Family](tag, "FAMILIES") {

  def id = column[Int]("ID", O.AutoInc, O.PrimaryKey)
  def name = column[String]("NAME")
  def createdAvatars = column[Option[Int]]("CREATED_AVATARS")
  def availableAvatars = column[Option[Int]]("AVAILABLE_AVATARS")

  def * = (name, createdAvatars, availableAvatars, id.?) <> (Family.tupled, Family.unapply)
}