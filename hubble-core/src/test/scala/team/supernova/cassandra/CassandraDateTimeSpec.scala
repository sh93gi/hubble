package team.supernova.cassandra

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.Matchers._
import org.scalatest.{FunSpecLike, Matchers}

class CassandraDateTimeSpec
    extends FunSpecLike {

    describe("CassandraDateTime") {

      it("should print midnight+timezone offset summertime") {
        val date = new DateTime(2016, 3, 30, 10, 0, 0, 0)
        CassandraDateTime.toDate(date) should be ("2016-03-30 00:00:00+0000")
      }

      it("should print midnight+timezone offset wintertime") {
        val date = new DateTime(2016, 3, 27, 10, 0, 0, 0)
        CassandraDateTime.toDate(date) should be ("2016-03-27 00:00:00+0000")
      }

      it("should print take into account a possible nondefault timezone") {
        val date = new DateTime(2016, 3, 30, 1, 0, 0, 0)
        CassandraDateTime.toDate(date, DateTimeZone.forID("Europe/Berlin")) should be ("2016-03-29 00:00:00+0000")
      }
    }

}
