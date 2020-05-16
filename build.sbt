import com.trueaccord.scalapb.{ScalaPbPlugin => PB}

name := "GameServer"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++=Seq(
  // actors
  "com.typesafe.akka" %% "akka-actor" % "2.4.11",
  // JDBC driver
  "org.postgresql" % "postgresql" % "9.4.1210",
  // Hikari - high-performance JDBC connection pool
  "com.typesafe.slick" % "slick-hikaricp_2.11" % "3.1.1",
  // config
  "com.typesafe" % "config" % "1.3.0",
  // Slick
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  // Slick-pg additional types
  "com.github.tminglei" %% "slick-pg" % "0.14.3",
  "com.github.tminglei" %% "slick-pg_spray-json" % "0.14.3",
  "com.github.tminglei" %% "slick-pg_joda-time" % "0.14.3",
  // JSON
  "io.spray" %%  "spray-json" % "1.3.2",
  // JodaTime for last online time
  "joda-time" % "joda-time" % "2.9.4"
)

PB.protobufSettings

PB.runProtoc in PB.protobufConfig := { args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v300" +: args.toArray)
}

scalaSource in PB.protobufConfig <<= (sourceDirectory in Compile)(_ / "generated" )

libraryDependencies ++= Seq(
  // For finding google/protobuf/descriptor.proto
  "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.5.29" % PB.protobufConfig
)

mainClass in Compile := Some("Main")