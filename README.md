## Hubble
Hubble is a 'telescope' into a group of cassandra clusters to help make life easier and reducing the need to log on to the cluster thus improving security even further.   



## Architecture:

![alt text](https://github.com/Supernova-Team/hubble/blob/master/doc/hubble.jpg)


## Getting Started

- Add maven parameters
```XML
<confluence.user>the user used for connecting to Confluence</confluence.user>
<confluence.password>the password used for connecting to Confluence</confluence.password>
<confluence.endpointaddress>the url of the Confluence SOAP endpoint</confluence.endpointaddress>
<confluence.space>the Confluence space where you want to write the pages to</confluence.space>
<confluence.group>the page group on the space you want to write to</confluence.group>

<hubble.cassandra.username>the user to connect to Cassandra</hubble.cassandra.username>
<hubble.cassandra.password>the password to connect to Cassandra</hubble.cassandra.password>
<hubble.cassandra.keyspace>the keyspace which you want to connect to</hubble.cassandra.keyspace>
<hubble.cassandra.host>hosts list (comma separated) for connecting to Cassandra</hubble.cassandra.host>
<hubble.cassandra.port>port used to connect to Cassandra (e.g. 9042)</hubble.cassandra.port>
```
- Add example insert statement
- Add table definition

## Packaging and running the application
Run ```Shell
 mvn clean install 
```
This will create a package for you at Hubble-core component
The generated jar file intentionally leaves out application.conf. This will help if you have different configuration for different environment.
You can run the jar by providing application.conf from outside. example:
```Shell
java -Dconfig.file='{some-location}/application.conf' -jar hubble-core-1.0-SNAPSHOT-jar-with-dependencies.jar
```



## Confluence 

If you don't have [Confluence](https://www.atlassian.com/software/confluence) already installed or want to setup a local development environment then we recommend using docker and setup a [Confluence container](https://hub.docker.com/r/cptactionhank/atlassian-confluence/). 

Please enable the following features in confluence otherwise the generated pages won't work: 

- [remote APIs](https://confluence.atlassian.com/display/DOC/Enabling+the+Remote+API)
- [HTML macros](https://confluence.atlassian.com/display/DOC/HTML+Macro) 


## Still TODO:
- Add configuration to get started.
- Make standalone jar (instead of test case to generate pages)
- Add list after presenting at Cassandra Summit 2015 :-)

