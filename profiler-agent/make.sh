#!/bin/bash

set -e

# Dummy makefile while we figure this out

JDK_PATH=/usr/lib/jvm/java-7-openjdk-amd64


mkdir -p target

g++ -fpic -I${JDK_PATH}/include/ -Isrc/main/include -c src/main/cpp/agent.cpp -o target/agent.o
g++ -fpic -I. -I${JDK_PATH}/include/ -Isrc/main/include -c src/main/cpp/profiledata.cc -o target/profiledata.o
g++ -fpic -I. -I${JDK_PATH}/include/ -Isrc/main/include -c src/main/cpp/java_profiler.cc -o target/java_profiler.o

g++ -fpic -shared  target/agent.o target/profiledata.o target/java_profiler.o -otarget/libprofiler.so

