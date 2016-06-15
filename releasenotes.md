
## 0.7
* In application.conf we renamed hubble.graphite.metrics to hubble.graphite.cluster_metrics
* In application.conf we added hubble.graphite.keyspace_metrics, allowing metrics to be retrieved per keyspace. See  application.conf.example for formatting details.
* The often changing metrics are written to separate confluence pages, without 'notify users'.