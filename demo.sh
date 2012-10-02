#!/bin/bash

# As we don't want to depend on maven-nar-plugin
pushd profiler-agent
./make.sh
popd

mvn package

java -cp profiler-example/target/profiler-example-1.0-SNAPSHOT.jar com.fathomdb.profiler.tests.Test1

# With maven-nar
#pushd profiler-agent/target/nar/profiler-agent-1.0-SNAPSHOT-amd64-Linux-gpp-jni/lib/amd64-Linux-gpp/jni
#LIBPATH=`pwd`
#popd

pushd profiler-agent/target
LIBPATH=`pwd`
popd

rm -f /tmp/profile.dat
rm -f /tmp/profile.dat

LD_LIBRARY_PATH=${LIBPATH} java -agentlib:profiler -javaagent:profiler-java/target/profiler-java-1.0-SNAPSHOT-jar-with-dependencies.jar  -cp profiler-example/target/profiler-example-1.0-SNAPSHOT.jar com.fathomdb.profiler.tests.Test1

#java -Djava.library.path=${LIBPATH} -javaagent:profiler-java/target/profiler-java-1.0-SNAPSHOT-jar-with-dependencies.jar -cp profiler-example/target/profiler-example-1.0-SNAPSHOT.jar com.fathomdb.profiler.tests.Test1

#java -jar profiler-java/target/profiler-java-1.0-SNAPSHOT-jar-with-dependencies.jar /tmp/profile.dat

dot -Tpdf /tmp/profile.dot -o/tmp/profile.pdf

# GhostView performance is _terrible_ on my machine / graphics card (?)
# Evince's scrolling is awkward for oversized documents
# XPDF is good but scrolling is painful
# Okular seems to work quite well
okular /tmp/profile.pdf

