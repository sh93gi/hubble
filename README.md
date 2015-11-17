## Hubble
Hubble is a 'telescope' into a group of cassandra clusters to help make life easier and reducing the need to log on to the cluster thus improving security even further.   



## Architecture:

![alt text](https://github.com/Supernova-Team/hubble/blob/master/doc/hubble.jpg)


## Getting Started

- Add maven parameters
    #Confluence
    confluence.user= the user used for connecting to Confluence
    confluence.password= the password used for connecting to Confluence
    confluence.endpointaddress= the url of the Confluence SOAP endpoint
    confluence.space= the Confluence space where you want to write the pages to
    confluence.group= the page group on the space you want to write to
    #Cassandra
    hubble.cassandra.username= the user to connect to Cassandra
    hubble.cassandra.password= the password to connect to Cassandra
    hubble.cassandra.keyspace= the keyspace which you want to connect to
    hubble.cassandra.hosts= hosts list (comma separated) for connecting to Cassandra
    hubble.cassandra.port= port used to connect to Cassandra (e.g. 9042)
- Add example insert statement
- Add table definition





## Confluence 

If you don't have [Confluence](https://www.atlassian.com/software/confluence) already installed or want to setup a local development environment then we recommend using docker and setup a [Confluence container](https://hub.docker.com/r/cptactionhank/atlassian-confluence/). 

Please enable the following features in confluence otherwise the generated pages won't work: 

- [remote APIs](https://confluence.atlassian.com/display/DOC/Enabling+the+Remote+API)
- [HTML macros](https://confluence.atlassian.com/display/DOC/HTML+Macro) 


## Still TODO:
- Add configuration to get started.
- Make standalone jar (instead of test case to generate pages)
- Add list after presenting at Cassandra Summit 2015 :-)

