# HDSLedger

## Introduction

HDSLedger is a simplified permissioned (closed membership) blockchain system with high dependability
guarantees. It uses the Istanbul BFT consensus algorithm to ensure that all nodes run commands
in the same order, achieving State Machine Replication (SMR) and guarantees that all nodes
have the same state.

## Requirements

- [Java 17](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html) - Programming language;

- [Maven 3.8](https://maven.apache.org/) - Build and dependency management tool;

- [Python 3](https://www.python.org/downloads/) - Programming language;

---

# Configuration Files

### Node configuration

Can be found inside the `resources/` folder of the `Service` module.

```json
{
    "id": <NODE_ID>,
    "hostname": "localhost",
    "port": <NODE_PORT>,
    "clientPort": <NODE_CLIENT_PORT>,
    "privKeyPath": <PATH_TO_PRIV_KEY>,
    "pubKeyPath": <PATH_TO_PUB_KEY>,
    "persona": <PERSONA>,
}
```

- `<NODE_ID>` - Must be a integer-like string (e.g. `"1"`), unique for each process;
- `<NODE_PORT>` - Must be an integer and unique in `localhost` - used to communicate with other nodes;
- `<NODE_CLIENT_PORT>` - Must be an integer and unique in `localhost` - used to communicate with the clients.
- `<PATH_TO_PUB_KEY>` - Must be a string with the path to the public key file - `$(pwd)` is `Service` module root;
- `<PATH_TO_PRIV_KEY>` - Must be a string with the path to the private key file - `$(pwd)` is `Service` module root;
- `<PERSONA>` - Must be a string with the _persona_ of the node - _ipsis verbis_ `Persona` enum in the `Service` module, (e.g. `"REGULAR"`).

### Client configuration

Can be found inside the `resources/` folder of the `Client` module.

```json
{
    "id": <CLIENT_ID>,
    "hostname": "localhost",
    "port": <NODE_PORT>,
    "privKeyPath": <PATH_TO_PRIV_KEY>,
    "pubKeyPath": <PATH_TO_PUB_KEY>,
}
```

- `<CLIENT_ID>` - Must be a string (e.g. `"1"`), unique for each process;
- Same as above for the rest of the fields.

## Dependencies

To install the necessary dependencies run the following command:

```bash
./install_deps.sh
```

This should install the following dependencies:

- [Google's Gson](https://github.com/google/gson) - A Java library that can be used to convert Java Objects into their JSON representation.
- [Apache Commons](https://commons.apache.org/) - A Java library with open source code for the Java platform, providing useful methods for common programming tasks.


## Puppet Master

The puppet master is a python script `puppet-master.py` which is responsible for starting the nodes
of the blockchain.
The script runs with `kitty` terminal emulator by default since it's installed on the RNL labs.

To run the script you need to have `python3` installed.
The script has arguments which can be modified:

- `terminal` - the terminal emulator used by the script
- `server_config` - a string from the array `server_configs` which contains the possible configurations for the blockchain nodes

Get instructions on how to run the script with the following command:

```bash
python3 puppet-master.py help
Usage: python puppet-master.py <server_config_id> <client_config_id>
```
Note: You may need to install **kitty** in your computer

## Maven

It's also possible to run the project manually by using Maven.

### Instalation

Compile and install all modules using:

```
mvn clean install
```

### Execution

Run without arguments

```
cd <module>/
mvn compile exec:java
```

Run with arguments

```
cd <module>/
mvn compile exec:java -Dexec.args="..."
```
---
This codebase was adapted from last year's project solution, which was kindly provided by the following group: [David Belchior](https://github.com/DavidAkaFunky), [Diogo Santos](https://github.com/DiogoSantoss), [Vasco Correia](https://github.com/Vaascoo). We thank all the group members for sharing their code.

