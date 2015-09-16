## Hubble
Hubble is a Cassandra Shared Cluster Dashboard

(doc/hubble.jpg)


Still TODO:
Make standalone jar (instead of test case to generate pages)





## Confluence 


If you don't have Confluence already installed somewhere or want to setup a local development environment then we recommend using docker:
[Confluence container](https://hub.docker.com/r/cptactionhank/atlassian-confluence/)  docker run --detach --name conf --publish 8090:8090 cptactionhank/atlassian-confluence:5.8.5 


Please enable the following features in confluence: 

[remote APIs](https://confluence.atlassian.com/display/DOC/Enabling+the+Remote+API)
[HTML macros](https://confluence.atlassian.com/display/DOC/HTML+Macro) 


