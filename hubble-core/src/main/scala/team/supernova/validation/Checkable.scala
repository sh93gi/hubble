package team.supernova.validation

trait Checkable {
  def checks: List[Check] = combineChecks(myChecks, children)

  def combineChecks(acc: List[Check], checkable: List[Checkable]): List[Check] = acc ++ checkable.flatMap(_.checks)

  protected def myChecks: List[Check]

  protected def children: List[Checkable]
}
