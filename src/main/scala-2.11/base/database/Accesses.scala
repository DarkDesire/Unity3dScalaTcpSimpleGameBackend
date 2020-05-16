package base.database

import slick.driver.PostgresDriver.api._

case class Access(name: String, id: Option[Int] = None)
object Traveler extends Access("Traveler")
object Explorer extends Access("Explorer")
object Conqueror extends Access("Conqueror")

// A Accesses table with 2 columns: id, name
class Accesses(tag: Tag) extends Table[Access](tag, "ACCESSES") {

  def id = column[Int]("ID", O.AutoInc, O.PrimaryKey)
  def name = column[String]("NAME")
  // TODO: add more columns ?
  def * = (name, id.?) <> (Access.tupled, Access.unapply)
}