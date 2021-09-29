# Sauron

Distributed Systems 2019-2020, 2nd semester project


## Authors

**Group A47**

### Code identification

In all the source files (including POMs), please replace __CXX__ with your group identifier.  
The group identifier is composed by Campus - A (Alameda) or T (Tagus) - and number - always with two digits.  
This change is important for code dependency management, to make sure that your code runs using the correct components and not someone else's.

### Team members

| Number | Name                    | User                             | Email                                 |
| -------|-------------------------|----------------------------------| --------------------------------------|
| 74224  | Pedro Magalhães         | <https://github.com/mavape>      | <mailto:ist174224@tecnico.ulisboa.pt> |
| 89454  | Guilherme Ribeiro       | <https://github.com/guimribeiro> | <mailto:ist189454@tecnico.ulisboa.pt> |
| 90739  | João Vieira             | <https://github.com/jvieiratpt>  | <mailto:ist190739@tecnico.ulisboa.pt> |

### Task leaders

| Task set | To-Do                         | Leader              |
| ---------|-------------------------------| --------------------|
| core     | protocol buffers, silo-client | _(whole team)_      |
| T1       | cam_join, cam_info, eye       | _Pedro Magalhães_   |
| T2       | report, spotter               | _João Vieira_       |
| T3       | track, trackMatch, trace      | _Guilherme Ribeiro_ |
| T4       | test T1                       | _João Vieira_       |
| T5       | test T2                       | _Guilherme Ribeiro_ |
| T6       | test T3                       | _Pedro Magalhães_   |


## Getting Started

The overall system is composed of multiple modules.
The main server is the _silo_.
The clients are the _eye_ and _spotter_.

See the [project statement](https://github.com/tecnico-distsys/Sauron/blob/master/README.md) for a full description of the domain and the system.

### Prerequisites

Java Developer Kit 11 is required running on Linux, Windows or Mac.
Maven 3 is also required.

To confirm that you have them installed, open a terminal and type:

```
javac -version

mvn -version
```

### Installing

To compile and install all modules:

```
mvn clean install -DskipTests
```

The integration tests are skipped because they require the servers to be running.


## Built With

* [Maven](https://maven.apache.org/) - Build Tool and Dependency Management
* [gRPC](https://grpc.io/) - RPC framework


## Versioning

We use [SemVer](http://semver.org/) for versioning. 
