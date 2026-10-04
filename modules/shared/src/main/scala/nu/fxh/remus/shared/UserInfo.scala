package nu.fxh.remus.shared

import sttp.tapir.codec.iron.given

case class UserInfo(id: UserId, username: UserName) derives Codec, Schema
