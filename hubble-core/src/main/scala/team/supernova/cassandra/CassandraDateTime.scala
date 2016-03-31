package team.supernova.cassandra

import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone, LocalDateTime}

object CassandraDateTime {
  /**
    * Prints a datetime such that it can be used in cassandra queries for date comparison
    * Cassandra stores dates using datetimes with midnight time, midnight at UTC.
    * Datetimes in queries and results are transformed into the clients timezone.
    * Therefore this query transforms your datetime, to midnight at UTC, written in your timezone.
    * Thus 2016-03-20 midnight at UTC = 2016-03-20 00:00:00+0000,
    *   which is the same moment as (logically equal to) 2016-03-20 02:00:00+0200
    * This returns the string representation of midnight at UTC with the date of the provided datetime.
    * @param datetime the moment which needs to be transformed into a 'date', similarly generated as cassandra
    * @param currentTimeZone the timezone applicable to the DateTime
    * @return a string
    */
  def toDate(datetime: DateTime, currentTimeZone: DateTimeZone = DateTimeZone.getDefault):String = {
    val tz =DateTimeZone.getDefault // Needs to be the zone our servers use, could also specify explicit "Europe/Berlin"
    val cassandra_date = new LocalDateTime(datetime.getMillis)
        .toDateTime(currentTimeZone)
        .toDateTime(DateTimeZone.UTC)
        .withTimeAtStartOfDay()
    val fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ")
    fmt.print(cassandra_date)
  }
}
