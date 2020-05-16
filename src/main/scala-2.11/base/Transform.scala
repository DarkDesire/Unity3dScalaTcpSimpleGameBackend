package base

import packet.{PacketPosition, PacketRotation, PacketTransform}
import spray.json._

////////////////////////////////////////////////////////////////////////////////////////
object MyJsonTransformProtocol extends DefaultJsonProtocol  {
  implicit object TransformJsonFormat extends RootJsonFormat[Transform] {
    def write(c: Transform) = JsObject(
      "position" -> JsObject(
        "x" -> JsNumber(c.position.x),
        "y" -> JsNumber(c.position.y),
        "z" -> JsNumber(c.position.z)
      ),
      "rotation" -> JsObject(
        "pitch" -> JsNumber(c.rotation.pitch),
        "yaw" -> JsNumber(c.rotation.yaw)
      )
    )
    def read(value: JsValue) = {
      value.asJsObject.getFields("position", "rotation") match {
        case Seq(JsObject(position),JsObject(rotation)) => {
          val x = position("x").asInstanceOf[JsNumber].value.toFloat
          val y = position("y").asInstanceOf[JsNumber].value.toFloat
          val z = position("z").asInstanceOf[JsNumber].value.toFloat
          val pitch = rotation("pitch").asInstanceOf[JsNumber].value.toFloat
          val yaw = rotation("yaw").asInstanceOf[JsNumber].value.toFloat
          new Transform(Position(x,y,z),Rotation(pitch,yaw))
        }
        case _ => throw new DeserializationException("Transform expected")
      }
    }
  }
}
////////////////////////////////////////////////////////////////////////////////////////
case class Position(var x:Float,var y:Float,var z:Float){
  override def toString: String = s"Position: X:$x, Y:$y, Z:$z. "
  def fromPosition(other:Position) = {
    x = other.x
    y = other.y
    z = other.z
  }
}
object Position{
  def default = new Position(0f,3f,0f)
  def fromPacketPosition(packetPosition: PacketPosition):Position = {
    new Position(packetPosition.x, packetPosition.y, packetPosition.z)
  }
}
////////////////////////////////////////////////////////////////////////////////////////
case class Rotation(var pitch:Float, var yaw:Float){
  override def toString: String = s"Rotation: Pitch:$pitch, Yaw:$yaw."
  def fromRotation(other: Rotation) = {
    pitch = other.pitch
    yaw = other.yaw
  }
}
object Rotation{
  def default = new Rotation(0f,0f)
  def fromPacketRotation(packetRotation: PacketRotation):Rotation = {
    new Rotation(packetRotation.pitch,packetRotation.yaw)
  }
}
////////////////////////////////////////////////////////////////////////////////////////
case class Transform(var position:Position, var rotation: Rotation){
  import MyJsonTransformProtocol._
  def returnJsonString: String = this.toJson.toString
  override def toString: String = s"${position} ${rotation}"

  def fromTransform(other: Transform) = {
    position = other.position
    rotation = other.rotation
  }
}
object Transform{
  def default = new Transform(Position.default,Rotation.default)
  def fromPacketTransform(packetTransform: PacketTransform) = {
    new Transform(Position.fromPacketPosition(packetTransform.position.head),Rotation.fromPacketRotation(packetTransform.rotation.head))
  }
}