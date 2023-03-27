pushd ..\..
java -Dtiming -Ddebug -classpath mbank.jar;jars\* MemoryBank %*
popd
