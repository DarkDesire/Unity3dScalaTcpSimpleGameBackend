package base

trait Value { def code: Int }

case object Cmd {
  case object CmdPing                 extends Value { val code = 1 }

  case object CmdLogin                extends Value { val code = 11 }
  case object CmdLoginSuccess         extends Value { val code = 12 }
  case object CmdLoginFailed          extends Value { val code = 13 }

  case object CmdSetFamilyRequest     extends Value { val code = 21 }
  case object CmdSetFamily            extends Value { val code = 22 }
  case object CmdSetFamilySuccess     extends Value { val code = 23 }
  case object CmdSetFamilyFailed      extends Value { val code = 24 }
  case object CmdPreviewAvatar        extends Value { val code = 25 }
  case object CmdPreviewAvatars       extends Value { val code = 26 }
  case object CmdSetNewAvatar         extends Value { val code = 27 }
  case object CmdSetNewAvatarSuccess  extends Value { val code = 28 }
  case object CmdSetNewAvatarFailed   extends Value { val code = 29 }

  case object CmdJoinToChannel        extends Value { val code = 31 }

  case object CmdAvatar               extends Value { val code = 41 }
  case object CmdAvatars              extends Value { val code = 42 }
  case object CmdJoinedAvatar         extends Value { val code = 43 }
  case object CmdAvatarMove           extends Value { val code = 44 }
  case object CmdAvatarAnimChange     extends Value { val code = 45 }
  case object CmdShoot                extends Value { val code = 46 }
  case object CmdShootToAvatar        extends Value { val code = 47 }
  case object CmdShootSuccess         extends Value { val code = 48 }
  case object CmdShootToAvatarSuccess extends Value { val code = 49 }
  case object CmdShootFailed          extends Value { val code = 50 }
  case object CmdKilledBy             extends Value { val code = 51 }
  case object CmdRespawn              extends Value { val code = 52 }

  case object CmdAvatarLeft           extends Value { val code = 61 }
  case object CmdBanned               extends Value { val code = 62 }
  case object CmdDisconnect           extends Value { val code = 63 }
}

case object Access {
  case object Traveler                extends Value { val code = 1 }
  case object Explorer                extends Value { val code = 2 }
  case object Conqueror               extends Value { val code = 3 }
}

case object AnimState {
  case object Idle                    extends Value { val code = 1 }
  case object Walking                 extends Value { val code = 2 }
  case object Die                     extends Value { val code = 3 }
}