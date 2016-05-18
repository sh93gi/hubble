package team.supernova.opscenter

case class OpsTableInfo(tableName: String, avgDataSizeMB: Long, numberSSTables: Long) extends Ordered[OpsTableInfo] {
  def compare(that: OpsTableInfo) = {
    this.tableName.compareTo(that.tableName)
  }
}
