package nu.fxh.remus.shared

enum WeightCategory derives Codec, Schema {

  case Light, Normal, Heavy

  def weight: Double = this match
    case Light  => 0.5
    case Normal => 1.0
    case Heavy  => 2.0

}
