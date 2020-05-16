package base
import spray.json._

object MyJsonMannaProtocol extends DefaultJsonProtocol {
  implicit object MannaJsonFormat extends RootJsonFormat[Manna] {
    def write(h: Manna) =
      JsArray(JsNumber(h.current), JsNumber(h.maximum))

    def read(value: JsValue) = value match {
      case JsArray(Vector(JsNumber(current), JsNumber(maximum))) =>
        new Manna(current.toInt, maximum.toInt)
      case _ => deserializationError("Manna expected")
    }
  }
}

case class Manna(var current: Int, var maximum: Int){
  import MyJsonMannaProtocol._
  val zero = 0

  override def toString: String = s"Current:${current}. Maximum:${maximum}."

  def returnJsonString: String = this.toJson.toString
  
  def changeManna(amount:Int) = {
    if ((current + amount) <= zero) { // below zero
      current = zero
    } else { // above zero
      if ((current+amount) > maximum) { // above maximum
        current = maximum
      } else {
        current += amount
      }
    }
  }
}

object Manna {
  val defaultMaxManna = 200
  def default = new Manna(defaultMaxManna,defaultMaxManna)
}