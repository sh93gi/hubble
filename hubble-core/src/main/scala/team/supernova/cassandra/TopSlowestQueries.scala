package team.supernova.cassandra

/**
  * @param limit the maximum number of queries ever queried for (limits memory usage)
  */
class TopSlowestQueries(limit: Option[Int]){
  val commands = scala.collection.mutable.Map[List[String], SlowQuery]()

  /**
    * Includes a new slow query. If the same commands have been seen before, keeps the slowest version
    *
    * @param queryDetails the representation of the slow query
    */
  def add(queryDetails: SlowQuery): Unit ={
    val current = commands.getOrElseUpdate(queryDetails.commands, queryDetails)
    commands.update(queryDetails.commands, SlowQuery.slowest(Seq(current, queryDetails)))
    limit match {
      case Some(x)=>
        if (commands.size>2 * x) {
          commands.--=(commands.toList.sortBy(-_._2.duration).takeRight(commands.size - x).map(_._1))
        }
      case _ =>
    }
  }

  /**
    * Returns the top num slowest queries
    *
    * @param num the number of queries to be returned
    * @return the slowest queries
    */
  def get(num: Int):List[SlowQuery] = {
    commands.values.toList.sortBy(-_.duration).take(num)
  }

  def get(): List[SlowQuery] = {
    get(limit.getOrElse(commands.size))
  }
}

