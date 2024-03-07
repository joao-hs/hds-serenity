#!/usr/bin/env python

import os
import json
import sys
import signal


# Terminal Emulator used to spawn the processes
terminal = "kitty"

# Blockchain node configuration file name
server_configs_dir = "Service/src/main/resources"
server_configs = os.listdir(server_configs_dir)

client_configs_dir = "Client/src/main/resources"
client_configs = os.listdir(client_configs_dir)

if len(sys.argv) != 3:
    print("Usage: python puppet-master.py <server_config_id> <client_config_id>")
    print("Server Configurations:")
    for i, config in enumerate(server_configs):
        print(f"\t{i+1}. {config}")
    print("Client Configurations:")
    for i, config in enumerate(client_configs):
        print(f"\t{i+1}. {config}")
    sys.exit()

server_config = server_configs[int(sys.argv[1]) - 1]
client_config = client_configs[int(sys.argv[2]) - 1]

def quit_handler(*args):
    os.system(f"pkill -i {terminal}")
    sys.exit()


# Compile classes
os.system("mvn clean install")

# Spawn blockchain nodes
with open(f"Service/src/main/resources/{server_config}") as f:
    data = json.load(f)
    processes = list()
    for key in data:
        pid = os.fork()
        if pid == 0:
            os.system(
                f"{terminal} sh -c \"cd Service; mvn exec:java -Dexec.args='{key['id']} {server_config} {client_config}' ; sleep 500\"")
            sys.exit()

with open(f"Client/src/main/resources/{client_config}") as f:
    data = json.load(f)
    processes = list()
    for key in data:
        pid = os.fork()
        if pid == 0:
            os.system(
                f"{terminal} sh -c \"cd Client; mvn exec:java -Dexec.args='{key['id']} {client_config} {server_config}' ; sleep 500\"")
            sys.exit()

signal.signal(signal.SIGINT, quit_handler)

while True:
    print("Type quit to quit")
    command = input(">> ")
    if command.strip() == "quit":
        quit_handler()
