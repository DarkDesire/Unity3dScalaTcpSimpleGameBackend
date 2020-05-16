package base.database

import slick.driver.PostgresDriver.api._


case class Account(var email:String, var pass:String, var familyId: Option[Int] = None, var accessId: Option[Int] = None, id: Option[Int] = None)

// A Accounts table with 5 columns: id, email, pass, familyId, accessId
class Accounts(tag: Tag) extends Table[Account](tag, "ACCOUNTS") {

  def id = column[Int]("ID", O.AutoInc, O.PrimaryKey)
  def email = column[String]("EMAIL")
  def pass = column[String]("PASS")
  def familyId = column[Option[Int]]("FAMILY_ID")
  def accessId = column[Option[Int]]("ACCESS_ID")

  def * = (email, pass, familyId, accessId, id.?) <> (Account.tupled, Account.unapply)

  def family = foreignKey("FAMILY_FK", familyId, TableQuery[Families])(_.id) // TODO: add more restrict on update, on delete
  def access = foreignKey("ACCESS_FK", accessId, TableQuery[Accesses])(_.id) // TODO: add more restrict on update, on delete
}