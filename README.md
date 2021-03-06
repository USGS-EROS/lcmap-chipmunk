# Chipmunk
[![Build Status](https://travis-ci.org/USGS-EROS/lcmap-chipmunk.svg?branch=develop)](https://travis-ci.org/USGS-EROS/lcmap-chipmunk)

Chipmunk enables time-series access to geospatial raster data.

You can use it to define, store, and retrieve collections of data
over HTTP.


## Deploying Chipmunk

Chipmunk is run as a Docker container so you don't have to worry
about installing GDAL or building an uberjar. It will automatically
create keyspaces and default tables if you give it priveleged
credentials.

```
docker run -p 5656:5656 
           -e HTTP_PORT=5656 \
     	   -e HTTP_TIMEOUT_MILLIS=615000 \
           -e DB_HOST=<cassandra host> \
	   -e DB_USER=<cassandra user> \
	   -e DB_PASS=<cassandra pw> \
	   -e DB_KEYSPACE=<cassandra keyspace> \
	   -e DB_READ_TIMEOUT_MILLIS=600000 \
	   -e GDAL_HTTP_TIMEOUT=615 \
	   -e Xms=4352m \
	   -e Xmx=4352m \
	   -it usgseros/lcmap-chipmunk:latest
```

Chipmunk is configured using these environment variables:

| ENV                      | Description                     |
| ------------------------ | --------------------------------|
| `HTTP_PORT`              | Chipmunk's HTTP listener        |
| `HTTP_TIMEOUT_MILLIS`    | Chipmunk's HTTP Client timeout  |
| `DB_HOST`                | Cassandra node (just one)       |
| `DB_USER`                | Cassandra username              |
| `DB_PASS`                | Cassandra password              |
| `DB_PORT`                | Cassandra cluster port          |
| `DB_KEYSPACE`            | Chipmunk's keyspace name        |
| `DB_READ_TIMEOUT_MILLIS` | Cassandra query timeout         |
| `GDAL_HTTP_TIMEOUT`      | Timeout (secs) for GDAL vsicurl | 
| `Xmx`                    | Maximum JVM memory              |
| `Xms`                    | Minimum JVM memory              |


## Running a Local Chipmunk

Start backing services and a Chipmunk instance using docker-compose. This will
log all container output to stdout; use ctrl-c to stop all containers.

```
bin/boot
```

Load presets for the registry and grid resources.

```
bin/seed
```

Load additional sample data, if desired.

```
bin/grab && ls test/nginx/data/**/*.tar | xargs bin/load
```

## Running Operationally

Chipmunk uses approximately 3.8GB to perform 10 simultaneous ingests.

The suggested execution parameters are therefore:

```java -server -Xms=4352m -Xmx=4352m -XX:+UseG1GC -jar chipmunk-x.x.x.jar```

This will run Chipmunk with 4.3GB memory using the server JVM compiler and the G1GC garbage collector.

It is important to set the minimum and maximum memory (Xms and Xmx) equal to one another after profiling
so the JVM can avoid heap resizing operations.

Inside a Docker container, the JVM may see more memory than is actually available to the cgroup, leading to out-of-memory conditions.  Setting Xms and Xmx solves this as well.

JVMs 1.8 build 121 and later may use -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap instead of the -Xms and -Xmx flags in the Docker ENTRYPOINT.

Chipmunk's Docker image currently runs JDK 1.7.

## Developing Chipmunk

You need to install a few dependencies before running Chipmunk locally.

* [Docker Compose](https://docs.docker.com/compose/install/)
* [GDAL 1.11.x](https://gdal.org)

### Checkout the repository

```
git clone git@github.com:USGS-EROS/lcmap-chipmunk.git
cd lcmap-chipmunk
```

Start backing services using Docker compose.

*Important: If you plan to run Chipmunk from a REPL, be sure to set the scale
to zero.* Otherwise, the instance you start will conflict with one configured
to start automatically with docker-compose.

```
docker-compose -f resources/docker-compose.yml up --scale chipmunk=0
```

Create a `profiles.clj` so you don't have to mess with environment
variables for local development.

```
cp profiles.example.clj profiles.clj
```

Run the tests.

```
lein test
```

Start a REPL (this automatically starts a Chipmunk instance).

```
lein repl
```

Finally, check `dev/user.clj` to learn more about loading
data using a REPL.


## Resources

Chipmunk provides a few basic resources to enable the storage
and retrieval of geospatial time-series data.

* Registry
* Grids
* Inventory
* Chips
* Sources

### Registry

The `/registry` resource provides data about the collections of data
contained by a Chipmunk instance.

Get info about all layers in a Chipmunk instance:

```
http localhost:5656/registry
```

To add data to the registry, POST a JSON document containing a list
of layers.

```
http POST localhost:5656/registry < resource/registry.ard.json
```

### Grids

The `/grid` resource provides data about how raw data's spatial segmentation.

A grid provides parameters that can be used with an RST (reflection,
scaling, translation) matrix.

```
http GET localhost:5656/grid
```

For your convenience, two sub-resources are provided that can be used to
determine how arbitrary points are translated into other points that align
to the grid.

Get the grid point for an arbitrary x and y coordinate.

```
http GET localhost:5656/grid/snap x==1631415 y==1829805
```

Get the grid points for units surrounding (and including) the given point.

```
http GET localhost:5656/grid/near x==1631415 y==1829805
```


### Inventory

The `/inventory` resource is used to add raster data (using an HTTP POST
request) or to retrieve info about what was ingested (using an HTTP GET).

To ingest data, POST a JSON document containing the URL to the raster data.

Please Note: If you are running Chipmunk inside a Docker container, you
must use the IP address of the NGINX server. This can be obtained and set
for later use by running:

```
export NGINX_HOST=`docker inspect -f "{{ .NetworkSettings.Networks.resources_lcmap_chipmunk.IPAddress }}" resources_nginx_1`
```

```
http POST localhost:5656/inventory \
     url=http://$NGINX_HOST/LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif
```

The response contains detailed information about the source and a list
of the chips that were extracted.

To retrieve this information again, perform a `GET` using the same URL.

```
http GET localhost:5656/inventory?url=http://$NGINX_HOST/LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif
```

### Chips

The `/chips` resource provides a way to retrieve raw raster data. It gets
a list of chips, encoded as JSON, in response to spatio-temporal queries.

```
http GET "localhost:5656/chips?ubid=lc08_srb1&x=1631415&y=1829805&acquired=1980/2020"
```

### Sources

The `/sources` resource is a performant subset of inventory data.  HTTP GET requests
with a `tile` parameter will return any sources that have been ingested for the given tile.

```
http GET "localhost:5656/sources?tile=003009"
```

[2]: https://httpie.org/#installation
