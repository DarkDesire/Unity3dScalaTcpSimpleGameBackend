import base.database._
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._

class DbSettings  {

  val db = Database.forConfig("db")

  try {
    // The query interface for the Accesses table
    val accesses: TableQuery[Accesses] = TableQuery[Accesses]
    // the query interface for the Accounts table
    val accounts: TableQuery[Accounts] = TableQuery[Accounts]
    // the query interface for the Avatars table
    val avatars: TableQuery[Avatars] = TableQuery[Avatars]
    // the query interface for the Families table
    val families: TableQuery[Families] = TableQuery[Families]

    db.run(DBIO.seq(

      (accesses.schema ++ accounts.schema ++ avatars.schema ++ families.schema).create,

      // Insert all accesses to Accesses table
      accesses += Traveler,         //1
      accesses += Explorer,         //2
      accesses += Conqueror,        //3

      // Insert all accounts to Accounts table
      accounts += Account("test1","test",None,Some(1)),
      accounts += Account("test2","test",None,Some(1)),
      accounts += Account("test3","test",None,Some(1)),
      accounts += Account("test4","test",None,Some(1))
    ))
  } finally db.close



}
