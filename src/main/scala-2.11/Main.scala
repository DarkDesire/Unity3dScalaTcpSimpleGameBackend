/**
  * Created by HomeOne on 30.10.2016.
  */
object Main extends App {
  if (args.nonEmpty) {
    if (args(0) == "setupDB"){
      val dbSettings = new DbSettings
      println(s"Finishing db settings")
    } else if(args(0)=="run") { // run 127.0.0.1 8080  // or // run 82.146.34.100 8080
      val server = new Server(args(1),args(2).toInt)
    }
  }
}
