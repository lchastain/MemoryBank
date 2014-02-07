pushd ..\src
javac -classpath ".;c:\jars\" -Xlint MemoryBank.java
jar cvf ..\mbank.jar *.class
call noclass
popd
