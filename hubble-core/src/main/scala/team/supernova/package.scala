package team

import com.datastax.driver.core.exceptions.ReadTimeoutException
import org.slf4j.LoggerFactory

package object supernova {
  class Supernova{}
  val logger = LoggerFactory.getLogger(classOf[Supernova])

  def using[A <: { def close(): Unit }, B](param: A)(f: A => B): B =
    try {
      f(param)
    } finally {
      param.close()
    }

  def retryExecute[T](command: =>T, retries: Int = 0): T ={
    try{
      return command
    } catch {
      case e: ReadTimeoutException => {
        if (retries==0)
          throw e
        else {
          Thread.sleep(1000)
          logger.warn("Got read time out, %d retries left.".format(retries), e)
          retryExecute(command, retries - 1)
        }
      }
    }
  }
}
