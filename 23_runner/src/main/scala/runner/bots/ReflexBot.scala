package runner.bots

import runner.*

/**
 * Детермінований рефлексивний бот: аналізує лише найближчий зріз.
 */
class ReflexBot(seed: Long = 202L) extends CyberBot:

  override def name: String = "ReflexBot"

  private val everything: Set[Action] =
    Set(Action.KeepRunning, Action.Duck, Action.Jump)

  // Які дії безпечні для однієї перешкоди на заданій висоті
  private def allowedFor(h: Height): Set[Action] = h match
    case Height.Low  => Set(Action.Jump)
    case Height.Mid  => Set(Action.Duck, Action.Jump)
    case Height.High => Set(Action.KeepRunning, Action.Duck) // стрибати заборонено

  // Перетин вимог усіх перешкод на смузі (порожня смуга дозволяє все)
  private def safeActions(slice: TunnelSlice, lane: Lane): Set[Action] =
    Height.values.toList
      .filter(h => slice.hasObstacleAt(lane, h))
      .foldLeft(everything)((acc, h) => acc & allowedFor(h))

  // Порядок уподобань: біг, потім присідання, потім стрибок
  private val preference: List[Action] =
    List(Action.KeepRunning, Action.Duck, Action.Jump)

  private def obstacleCount(slice: TunnelSlice, lane: Lane): Int =
    Height.values.count(h => slice.hasObstacleAt(lane, h))

  // Маневр на сусідню смугу; Lane.left/right дають None за межею тунелю
  private def dodge(slice: TunnelSlice, lane: Lane): Action =
    val options: List[(Lane, Action)] = List(
      lane.left.map(l => (l, Action.MoveLeft)),
      lane.right.map(l => (l, Action.MoveRight))
    ).flatten

    options
      .filter { case (l, _) => safeActions(slice, l).contains(Action.KeepRunning) }
      .sortBy { case (l, _) => obstacleCount(slice, l) } // спершу порожню смугу
      .headOption
      .map { case (_, action) => action }
      .getOrElse(Action.KeepRunning) // маневр неможливий

  override def decide(observation: Observation): Action =
    observation.upcoming.headOption match
      case None => Action.KeepRunning
      case Some(slice) =>
        val lane = observation.hero.lane
        val here = safeActions(slice, lane)
        preference.find(here.contains).getOrElse(dodge(slice, lane))
