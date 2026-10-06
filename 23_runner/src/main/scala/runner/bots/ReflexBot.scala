package runner.bots

import runner.*

class ReflexBot(seed: Long = 202L) extends CyberBot:

  override def name: String = "ReflexBot"

  private val everything: Set[Action] =
    Set(Action.KeepRunning, Action.Duck, Action.Jump)

  // Safe actions for one obstacle at the given height
  private def allowedFor(h: Height): Set[Action] = h match
    case Height.Low  => Set(Action.Jump)
    case Height.Mid  => Set(Action.Duck, Action.Jump)
    case Height.High => Set(Action.KeepRunning, Action.Duck)

  // Intersection of requirements of all obstacles in the lane
  private def safeActions(slice: TunnelSlice, lane: Lane): Set[Action] =
    Height.values.toList
      .filter(h => slice.hasObstacleAt(lane, h))
      .foldLeft(everything)((acc, h) => acc & allowedFor(h))

  private val preference: List[Action] =
    List(Action.KeepRunning, Action.Duck, Action.Jump)

  private def obstacleCount(slice: TunnelSlice, lane: Lane): Int =
    Height.values.count(h => slice.hasObstacleAt(lane, h))

  // Move to a neighbour lane; Lane.left/right return None at the tunnel edge
  private def dodge(slice: TunnelSlice, lane: Lane): Action =
    val options: List[(Lane, Action)] = List(
      lane.left.map(l => (l, Action.MoveLeft)),
      lane.right.map(l => (l, Action.MoveRight))
    ).flatten

    val safe = options.filter { case (l, _) => safeActions(slice, l).contains(Action.KeepRunning) }
    val sorted = safe.sortBy { case (l, _) => obstacleCount(slice, l) }
    sorted.headOption match
      case Some((_, action)) => action
      case None              => Action.KeepRunning

  override def decide(observation: Observation): Action =
    observation.upcoming.headOption match
      case None => Action.KeepRunning
      case Some(slice) =>
        val lane = observation.hero.lane
        val here = safeActions(slice, lane)
        preference.find(here.contains).getOrElse(dodge(slice, lane))