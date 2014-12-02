Coopr-CLI
==================

 The CLI is used to manage Coopr from the command line.

## Supported Actions

 - manage Cluster Templates, Providers, Services, Hardware Types and Image Types with ```Admin CLI commands```;
 - manage Clusters with ```Cluster CLI commands```;
 - manage Plugins with ```Plugin CLI commands```;
 - manage Provisioners with ```Provisioner CLI commands```;
 - manage Tenants with ```Tenant CLI commands```; 

## Build
 
 To build the CLI, run the following command, from the parent directory:
 
 ```mvn clean package -pl coopr-cli -am``` or ``` mvn clean package -DskipTests -pl coopr-cli -am```

## Start

 To start the CLI, run the following command:
 
 ```java -jar target/coopr-cli-{version}.jar```
 
 ```{version}``` - the version of Coopr    
       
## Example
   
 When the CLI starts, you will see ```coopr (http://localhost:55054)> ```. 
 This is because the CLI is using its default parameters: 
 
  host - ```localhost```
  port - ```55054```
  ssl - ```false```
  user - ```admin```
  tenant - ```superadmin```
  
 To connect to another Coopr server, use the connect command:
 
 ```connect to <host> as <user> <tenant> [using port <port>] [with ssl <disabled | enabled>]```
 
 For example:
 
 ```connect to com.example as admin superadmin with ssl enabled```
 
 Use the help command to see a list of all commands:
  
 ```help```
 
 For example, to list all cluster templates available to the user:
 
 ```list templates```
 