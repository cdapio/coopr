Coopr-CLI
==================

The CLI is for managing Coopr from command line.

## Supported Actions

 - manage Cluster Templates, Providers, Services, Hardware Types and Image Types with ```Admin CLI commands```;
 - manage Clusters with ```Cluster CLI commands```;
 - manage Plugins with ```Plugin CLI commands```;
 - manage Provisioners with ```Provisioner CLI commands```;
 - manage Tenants with ```Tenant CLI commands```; 

## Build
 
 To build the Coopr CLI jar, use:
 
 ```mvn package``` or ``` mvn package -DskipTests```

## Start

 To start the Coopr CLI jar, use  (from Coopr project directory)
 
 ```java -jar coopr-cli/target/coopr-cli-{version}.jar```
 
 ```{version}``` - the version of Coopr    
       
## Example
   
 When Coopr CLI started, you would see ```coopr (http://localhost:55054)> ``` - CLI with default parameters: 
 
  host - ```localhost```
  port - ```55054```
  ssl - ```false```
  user - ```admin```
  tenant - ```superadmin```
  
 For reconnecting to another Coopr server use connect command:
 
 ```connect to <%s> as <%s> <%s> [using port <%s>] [with ssl <%s>]```
 
 For example:
 
 ```connect to com.example as admin superadmin with ssl enabled```
 
 Use help command to see list of all commands:
  
 ```help```
 
 Command example, list of the cluster templates available for the user: 
 
 ```list templates```
 