
## 0.7
* In application.conf one can add warnings or errros to hubble.graphite.cluster_metrics and hubble.graphite.keyspace_metrics.
  When a threshold is met, the warning/error will be added to the relevant cluster/keyspace page
* In application.conf we added hubble.graphite.keyspace_metrics, allowing metrics to be retrieved per keyspace. See  application.conf.example for formatting details.
* The often changing metrics are written to separate confluence pages, without 'notify users'.
### Breaking changes:
* In application.conf we renamed hubble.graphite.metrics to hubble.graphite.cluster_metrics
