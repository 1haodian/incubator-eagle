
## Prerequisites

* [Apache Maven](https://maven.apache.org/)
* [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

## Documentation


## Build

    mvn install
    
## Setup
The alert engine have three dependency module: Coordinator Service, Metadata Service, and engine runtime(storm topologies).

####0. Dependencies
> Alert engine need kafka as data source, ZK as coordination. Check alert-devtools/bin to start zk and kafka through start-zk-kafka.sh.

####1. Start metadata service
> For local dev, project alert-metadata-service packaging as a war, and enabled mvn jetty:run to run it. By default, metadata runs on localhost:8080

> For deployment, after mvn install, a war is avaialble in alert-metadata-service/target

####2. Start coordiantor service
> For local dev, project alert-coordinator packaing as a war, and enabled mvn jetty:run to run it. By default, it runs in localhost:9090, and have dependency on metadata. See application.conf for coordinator.

> For deployment, find war in alert-coordinator/target after mvn install

####3. Start engine runtime.
> The engine are the topologies that runs in any storm (local or remote) with configuration to connect to the ZK and metadata service. The alert engine runtime main as in UnitTopologyMain.java. The started storm bolt should have the same name described in alert-metadata. Example of the configuration is /alert-engine-base/src/main/resources/application.conf 

See below detailed steps.


## Run
* pre-requisites
  * zookeeper
  * storm
  * kafka
  * tomcat
  * mongdb

* Run Metadata service
    1. copy alert-metadata/target/alert-metadata-0.0.1-SNAPSHOT.war into tomcat webapps/alertmetadata.war
    2. check config under webapps/alertmetadata/WEB-INF/classes/application.conf
    ```json
    {
	"datastore": {
		"metadataDao": "org.apache.eagle.alert.metadata.impl.MongoMetadataDaoImpl",
		"connection": "localhost:27017"
	}
     }
    ```
    
    3. start tomcat
    
* Run Coordinator service
    1. copy alert-coordinator/target/alert-coordinator-0.0.1-SNAPSHOT.war to tomcat webappes/coordinator.war
    2. check config under webapps/coordinator/WEB-INF/classes/application.conf
    ```json
      {
	"coordinator" : {
		"policiesPerBolt" : 5,
		"boltParallelism" : 5,
		"policyDefaultParallelism" : 5,
		"boltLoadUpbound": 0.8,
		"topologyLoadUpbound" : 0.8,
		"numOfAlertBoltsPerTopology" : 5,
		"zkConfig" : {
			"zkQuorum" : "localhost:2181",
			"zkRoot" : "/alert",
			"zkSessionTimeoutMs" : 10000,
			"connectionTimeoutMs" : 10000,
			"zkRetryTimes" : 3,
			"zkRetryInterval" : 3000
		},
		"metadataService" : {
			"host" : "localhost",
			"port" : 8080,
			"context" : "/alertmetadata/api"
		},
		"metadataDynamicCheck" : {
			"initDelayMillis" : 1000,
			"delayMillis" : 30000
		}
	}
   }
   ```
    3. start tomcat

* Run UnitTopologyMain
    1. copy alert-assembly/target/alert-engine-0.0.1-SNAPSHOT-alert-assembly.jar to somewhere close to your storm installation
    2. check config application.conf
   ```json
  {
  "topology" : {
    "name" : "alertUnitTopology_1",
    "numOfTotalWorkers" : 2,
    "numOfSpoutTasks" : 1,
    "numOfRouterBolts" : 4,
    "numOfAlertBolts" : 10,
    "numOfPublishTasks" : 1,
    "messageTimeoutSecs": 3600,
    "localMode" : "true"
  },
  "spout" : {
    "kafkaBrokerZkQuorum": "localhost:2181",
    "kafkaBrokerZkBasePath": "/brokers",
    "stormKafkaUseSameZkQuorumWithKafkaBroker": true,
    "stormKafkaTransactionZkQuorum": "",
    "stormKafkaTransactionZkPath": "/consumers",
    "stormKafkaEagleConsumer": "eagle_consumer",
    "stormKafkaStateUpdateIntervalMs": 2000,
    "stormKafkaFetchSizeBytes": 1048586,
  },
  "zkConfig" : {
    "zkQuorum" : "localhost:2181",
    "zkRoot" : "/alert",
    "zkSessionTimeoutMs" : 10000,
    "connectionTimeoutMs" : 10000,
    "zkRetryTimes" : 3,
    "zkRetryInterval" : 3000
  },
  "dynamicConfigSource" : {
    "initDelayMillis": 3000,
    "delayMillis" : 10000
  },
  "metadataService": {
    "context" : "/alertmetadata/api",
    "host" : "localhost",
    "port" : 8080
  },
  "coordinatorService": {
    "host": "localhost",
    "port": 8080,
    "context" : "/coordinator/api"
  }
  "metric": {
    "sink": {
      "stdout": {}
    }
  }
}
```
  Note: please make sure the above configuration is used by storm instead of the configuration within fat jar
  3. start storm
     storm jar alert-engine-0.0.1-SNAPSHOT-alert-assembly.jar org.apache.eagle.alert.engine.UnitTopologyMain

## Support
