## Hubble
Hubble is a 'telescope' into a group of cassandra clusters to help make life easier and reducing the need to log on to the cluster thus improving security even further.   

Hubble collects the following data from your cluster, and creates a Confluence tree with the results:

 - Top slowest queries per cluster, keyspace and table, retrieved from dse_perf.node_slow_log if available.
 - Adds a graphite graph at the top of your keyspace confluence page showing the keyspace usage.
 - A summary of opscenter cluster metadata (like nr of sstables)
 - It uses cassandra's session.getCluster().getMetadata to get most information


## Architecture:

![alt text](https://github.com/Supernova-Team/hubble/blob/master/doc/hubble.jpg)


## Getting Started
This project uses scala config. You need to setup ```application.conf``` in the resource folder. 
There is ```application.conf.example``` file available in the resource folder that explains how to do this. ```application.conf.example``` is in typesafe HOCON format. 
Locations that this file should be inserted are: 
```
hubble-core/src/main/resources
hubble-core/src/test/resources
hubble-reporting/src/test/resources
```
By adding to ```hubble-core/src/main/resources```, you can run the application locally. Use ```ClusterInfoApp``` located in ```hubble-core``` as main.
By adding to ```hubble-core/src/test/resources``` and ```hubble-reporting/src/test/resources``` you can test the whole application.

## Packaging and running the application
after adding ```application.conf``` to test resources, run 
```
 mvn clean install 
```
This will create a package for you at target in Hubble-core component. 
The generated jar file, intentionally leaves out ```application.conf```. 
In case of having different configuration for different environments, this feature becomes very handy.
You can run the jar by providing ```application.conf``` from outside. example:
```
java -Dconfig.file='{some-location}/application.conf' -jar hubble-core-1.0-SNAPSHOT-jar-with-dependencies.jar
```



## Confluence 

If you don't have [Confluence](https://www.atlassian.com/software/confluence) already installed or want to setup a local development environment then we recommend using docker and setup a [Confluence container](https://hub.docker.com/r/cptactionhank/atlassian-confluence/). 

Please enable the following features in confluence otherwise the generated pages won't work: 

- [remote APIs](https://confluence.atlassian.com/display/DOC/Enabling+the+Remote+API)
- [HTML macros](https://confluence.atlassian.com/display/DOC/HTML+Macro) 




