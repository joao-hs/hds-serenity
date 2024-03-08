#!/usr/bin/env bash

mvn install:install-file -Dfile=dependencies/gson-2.10.1.jar -DgroupId="com.google.code.gson" -DartifactId="gson" -Dversion="2.10.1" -Dpackaging="jar"
mvn install:install-file -Dfile=dependencies/commons-lang3-3.12.0.jar -DgroupId="org.apache.commons" -DartifactId="commons-lang3" -Dversion="3.12.0" -Dpackaging="jar"