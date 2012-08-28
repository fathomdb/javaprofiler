#!/bin/bash

set -e

# Dummy makefile while we figure this out

JDK_PATH=/usr/lib/jvm/java-7-openjdk-amd64

g++ -fpic -I${JDK_PATH}/include/ -c agent.cpp -o agent.o
g++ -fpic -I. -I${JDK_PATH}/include/ -c profiledata.cc -o profiledata.o
g++ -fpic -I. -I${JDK_PATH}/include/ -c java_profiler.cc -o java_profiler.o

g++ -fpic -shared  agent.o profiledata.o java_profiler.o -olibprofiler.so

