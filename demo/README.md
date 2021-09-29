# Sauron Demo Guide

Distributed Systems 2019-2020, 2nd semester project

## Executing the server
```
mvn exec:java
```

## Executing the eye client, demonstration
```
/path/to/the/bin/eye localhost
> invalid number of arguments
/path/to/the/bin/eye localhost aa bb cc dd ee
> invalid number of arguments
/path/to/the/bin/eye localhost AA cam1 1.40 2.50
> invalid port format
/path/to/the/bin/eye localhost 2181 cam2 aa bb
> invalid coordinates
/path/to/the/bin/eye localhost 2181 cam3 1.40 2.50
$ person,AA11AA
$
> INVALID_ARGUMENT: Invalid IDs - person id: AA11AA
$ person,11111111111111111111111111111111111111
$ person,1234
$ car,1234
$ car,AA11AA
$ person,1234
$
> INVALID_ARGUMENT: Invalid IDs - person id: 11111111111111111111111111111111111111; car id: 1234
$ person,1111
$ person,aaaaa
'EOF'
```

## Executing the spotter, demonstration
```
/path/to/the/bin/spotter aa
> invalid number of arguments
/path/to/the/bin/spotter aa bb cc dd
> invalid number of arguments
/path/to/the/bin/spotter localhost VV
> invalid port format
/path/to/the/bin/spotter localhost 2181
$ spot person 1234
> person,1234,2020-04-03T15:07:54,cam3,1.40,2.50
$ spot person 1*
> invalid command
$ spot person 1*
> person,1111,2020-04-03T15:08:25,cam3,1.40,2.50
> person,1234,2020-04-03T15:07:54,cam3,1.40,2.50
$ trail person 1234
> person,1234,2020-04-03T15:08:10,cam3,1.40,2.50
> person,1234,2020-04-03T15:07:54,cam3,1.40,2.50
'EOF'
```

## Replicas, demonstration
Run two replicas, in different terminals
```
mvn exec:java

mvn exec:java -Dinstance=2
```

Run one eye
```
/path/to/the/bin/eye localhost 2181 cam1 10.0 10.0 1
$ # eye connected to the first instance
$ person,1
$ person,2
$ person,3
$
```

Run one spotter
```
/path/to/the/bin/spotter localhost 2181 2
> SpotterApp
> Run the command 'cache_limit <VALUE>' to define the maximum cache size
> Press <ENTER> to keep the maximum cache size to its default value (5)
$
> Maximum cache size set to its default value
$ # spotter connected to the second instance
$ # run the following command before the default gossip interval pass
$ spot person 1
> **no results**
$ # wait the necessary time to the replicas gossip and run the following command
$ spot person 1
> person,1,<timestamp>,cam1,10.0,10.0
```

## Fault tolerance, demonstration
Run two replicas, in different terminals
```
mvn exec:java

mvn exec:java -Dinstance=2
```

Run one eye
```
/path/to/the/bin/eye localhost 2181 cam1 10.0 10.0
$ person,1
$
$ # now kill the instance that the eye is connected to
$ person,2
$ # the eye connected to the second instance automatically
$
```

Run one spotter
```
/path/to/the/bin/spotter localhost 2181
> SpotterApp
> Run the command 'cache_limit <VALUE>' to define the maximum cache size
> Press <ENTER> to keep the maximum cache size to its default value (5)
$
> Maximum cache size set to its default value
$ # cache size is customizable by running the command cache_limit when starting the spotter client
$ # the spotter has connected with the second instance (the only that is opened)
$ spot person 2
> person,2,<timestamp>,cam1,10.0,10.0
$ # kill the second instance and open a third instance
$ spot person 2
> person,2,<timestamp>,cam1,10.0,10.0
$ # the third instance has zero knowledge of that observation,
$ # but the spotter had it saved in cache
```