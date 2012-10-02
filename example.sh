#!/bin/bash

mvn package

java -cp profiler-example/target/profiler-example-1.0-SNAPSHOT.jar com.fathomdb.profiler.tests.Test1

pushd profiler-agent/target/nar/profiler-agent-1.0-SNAPSHOT-amd64-Linux-gpp-jni/lib/amd64-Linux-gpp/jni
LIBPATH=`pwd`
popd

#LD_LIBRARY_PATH=${LIBPATH} 
java -Djava.library.path=${LIBPATH} -javaagent:profiler-java/target/profiler-java-1.0-SNAPSHOT-jar-with-dependencies.jar -cp profiler-example/target/profiler-example-1.0-SNAPSHOT.jar com.fathomdb.profiler.tests.Test1

