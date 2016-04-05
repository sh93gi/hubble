package team.supernova.cassandra

/**
  * @param limit the maximum number of queries ever queried for (limits memory usage)
  */
class TopSlowestQueries(limit: Option[Int]){
  val commands = scala.collection.mutable.Map[String, SlowQuery]()

  /**
    * Includes a new slow query. If the same commands have been seen before, keeps the slowest version
    *
    * @param queryDetails the representation of the slow query
    */
  def add(queryDetails: SlowQuery): Unit ={
    val queryKey = queryDetails.commands.mkString("\n")
    val current = commands.getOrElseUpdate(queryKey, queryDetails)
    commands.update(queryKey, SlowQuery.slowest(Seq(current, queryDetails)))
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
    * @param num the number of queries requested to be returned, is limited to at most limit (if isDefined)
    * @return the min(num, this.limit) slowest queries, ordered by duration descending.
    *         if limit is not defined, returns num, if that amount is available
    */
  def get(num: Int):List[SlowQuery] = {
    commands.values.toList.sortBy(-_.duration).take(Math.min(num, limit.getOrElse(num)))
  }

  /**
    * Returns the top this.limit slowest queries, if available. If limit is not defined, returns everything
    *
    * @return the this.limit slowest queries, ordered by duration descending
    */
  def get(): List[SlowQuery] = {
    get(limit.getOrElse(commands.size))
  }
}

