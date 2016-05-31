package team

import com.datastax.driver.core.exceptions.ReadTimeoutException
import org.slf4j.LoggerFactory

package object supernova {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def using[A <: { def close(): Unit }, B](param: A)(f: A => B): B =
    try {
      f(param)
    } finally {
      param.close()
    }

  /**
    * Retries the command at most retries time when ReadTimeoutExceptions occur.
    * After retries times rethrows the exception
    * @param command the operation to try
    * @param retries the max number of times to retry the operation
    * @tparam T the resulttype
    * @return the result of the succesfull execution of the command
    */
  def retryExecute[T](command: =>T, retries: Int = 0, sleepTime: Int = 1000): T ={
    try{
      command
    } catch {
      case e: ReadTimeoutException => {
        if (retries==0)
          throw e
        else {
          logger.warn(s"Got read time out, $retries retries left, so sleeping $sleepTime: ${e.getMessage}.")
          Thread.sleep(sleepTime)
          retryExecute(command, retries - 1, sleepTime)
        }
      }
    }
  }
}
