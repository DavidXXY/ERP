#!/bin/bash
cd /Users/mac/Documents/ERP/services/api
export JAVA_HOME=/Users/mac/Documents/ERP/.dev/tools/jdk-17.0.19+10/Contents/Home
mvn spring-boot:run -Dspring-boot.run.profiles=local -q
