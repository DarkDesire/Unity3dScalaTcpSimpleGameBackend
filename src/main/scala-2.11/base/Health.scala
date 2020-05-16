package base
import spray.json._

object MyJsonHealthProtocol extends DefaultJsonProtocol {
  implicit object HealthJsonFormat extends RootJsonFormat[Health] {
    def write(h: Health) =
      JsArray(JsNumber(h.current), JsNumber(h.maximum))

    def read(value: JsValue) = value match {
      case JsArray(Vector(JsNumber(current), JsNumber(maximum))) =>
        new Health(current.toInt, maximum.toInt)
      case _ => deserializationError("Health expected")
    }
  }
}

case class Health(var current: Int, var maximum: Int) {
  import MyJsonHealthProtocol._

  val zero = 0

  def returnJsonString: String = this.toJson.toString

  def IsDead:Boolean = {
    if (current == zero) true else false
  }
  def IsAlive = !IsDead

  def changeHealth(amount:Int) = {
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

  override def toString: String = s"Current:${current}. Maximum:${maximum}."
}

object Health {
  val defaultMaxHealth = 500
  def default = new Health(defaultMaxHealth,defaultMaxHealth)
}