coopr-rest-client
==================

The Rest Client Java API is for managing Coopr from Java applications.

## Supported Actions

 - manage Cluster Templates, Providers, Services, Hardware Types and Image Types with ```AdminClient```;
 - manage Clusters with ```ClusterClient```;
 - manage Plugins with ```PluginClient```;
 - manage Provisioners with ```ProvisionerClient```;
 - manage Tenants with ```TenantClient```; 

## Build
 
 To build the Coopr REST Client Java API jar, use:
 
 ```mvn package``` or ``` mvn package -DskipTests```

## Usage

 To use the Coopr REST Client Java API, include this Maven dependency in your project's ```pom.xml``` file:
 
 <dependency>
  <groupId>co.cask</groupId>
  <artifactId>coopr-rest-client</artifactId>
  <version>0.9.10-SNAPSHOT</version>
 </dependency>
            
## Example
   
 Create a ClientManager instance, specifying the fields 'host' and 'port' of the Coopr server.
 The 'userId' and the 'tenantId' also are mandatory fields. If these fields were not specified, will throw 
 ```IllegalArgumentException```. 
 Optional configurations that can be set (and their default values):
  
  - ssl: false (use HTTP protocol) 
  - version : 'v2' (Coopr server version, used as a part of the base URI [http(s)://localhost:55054/v2/...]) 
  - apiKey:  null (Need to specify to authenticate client requests)
  - verifySSLCert: true (By default, SSL Certificate verification is enabled)
  - gson: new Gson() (Gson instance with the default configurations) 
   
 ```
   ClientManager clientManager = RestClientManager.builder("localhost", 55054)
       .userId("admin")
       .tenantId("superadmin")
       .build();
 ```
      
 or specified optional parameters using builder:
 
 ```
   StreamClient streamClient = new RestStreamClient.Builder("localhost", 55054)
         .apiKey("apiKey")
         .ssl(true)
         .verifySSLCert(false)
         .version("v2")
         .build();
 ```
 
 Get an ```AdminClient``` singleton from the ```ClientManager```:
 
 ```
   AdminClient adminClient = clientManager.getAdminClient();
 ```
 Get all the cluster templates available for the user:     
 
 ```
  List<ClusterTemplates> result = adminClient.getAllClusterTemplates();
 ```
   
 When you are finished, release all resources by calling following methods:
  
 ```  
   clientManager.close();
 ```

## Additional Notes
 
 All client methods throw exceptions using response code analysis from the Coopr server. 
 These exceptions help determine if the request was processed successfully or not.
 
 In the case of a **200 OK** response, no exception will be thrown; other cases will throw ```HttpFailureException```
 with appropriate message and status code inside. 
 
