# Testing

There isn't an automated way of testing the code yet.
The only way to test the code is to run the code and see if it works as expected.

In the `scenarios` subdirectory, you can find some guides on how to test the scenarios described in the report.
Each script looks like this:
```
# config: <node configuration file> <client configuration file>

# this is a comment

<process's terminal to use>: <command to run>

# Example
C1: balance C2
C2: transfer C1 10 .4
```
You may copy and paste it in the corresponding terminal.
The first line indicates which node and client configuration to use.

## Example
1. Look at the scenario's configuration files you want to test

2. List the configuration files

```
$ python puppet-master.py help
```

Output:
```
Usage: python puppet-master.py <server_config_id> <client_config_id>
Server Configurations:
    <server_config_id>. <server_config_name>.json
    ...
Client Configurations:
    <client_config_id>. <client_config_name>.json
    ...
```

3. Run with the corresponding configuration files
```
$ python puppet-master.py <server_config_id> <client_config_id>
```

4. Copy and paste the commands in the corresponding terminal (in order)

5. Check for the relevant output in the corresponding terminal