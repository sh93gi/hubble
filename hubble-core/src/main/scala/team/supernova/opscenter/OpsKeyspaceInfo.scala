package team.supernova.opscenter

case class OpsKeyspaceInfo(keyspaceName: String, opsTableInfoList: List[OpsTableInfo]) extends Ordered[OpsKeyspaceInfo] {
  def compare(that: OpsKeyspaceInfo) = {
    this.keyspaceName.compareTo(that.keyspaceName)
  }
}

