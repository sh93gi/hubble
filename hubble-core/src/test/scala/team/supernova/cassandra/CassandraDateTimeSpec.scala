package team.supernova.cassandra

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.FunSpecLike
import org.scalatest.Matchers._

class CassandraDateTimeSpec
    extends FunSpecLike {

    describe("CassandraDateTime") {

      it("should represent midnight in summertime") {
        val date = new DateTime(2016, 3, 30, 10, 0, 0, 0)
        CassandraDateTime.toMidnight(date) should be
        be (new DateTime(2016, 3, 30, 0, 0, DateTimeZone.UTC))
      }

      it("should print midnight in wintertime") {
        val date = new DateTime(2016, 3, 27, 10, 0, 0, 0)
        CassandraDateTime.toMidnight(date) should be
        be (new DateTime(2016, 3, 27, 0, 0, DateTimeZone.UTC))
      }

      it("should print take into account a possible nondefault timezone") {
        val date = new DateTime(2016, 3, 30, 1, 0, 0, 0)
        CassandraDateTime.toMidnight(date, DateTimeZone.forID("Europe/Berlin")) should
          be (new DateTime(2016, 3, 29, 0, 0, DateTimeZone.UTC))
      }
    }

}
