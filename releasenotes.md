## 0.8
* In application.conf the hubble.cassandra.clusters.envs.graphite field has changed from a dictionary to an array of dictionaries.
  This allows multiple graphite instances, useful for the case when the metrics of keyspaces are spread over multiple graphites (one keyspace to one, another keyspace on another)
  Hubble will try them all, using the first graphite url which returns non empty data.
* The opscenter urls now require the http or https prefix
## 0.7
* In application.conf one can add warnings or errros to hubble.graphite.cluster_metrics and hubble.graphite.keyspace_metrics.
  When a threshold is met, the warning/error will be added to the relevant cluster/keyspace page
* In application.conf we added hubble.graphite.keyspace_metrics, allowing metrics to be retrieved per keyspace. See  application.conf.example for formatting details.
* The often changing metrics are written to separate confluence pages, without 'notify users'.
### Breaking changes:
* In application.conf we renamed hubble.graphite.metrics to hubble.graphite.cluster_metrics
