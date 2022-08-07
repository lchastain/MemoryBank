pushd ..
java -Dtiming -Ddebug -classpath mbank.jar;c:\jars\* MemoryBank %*
popd
