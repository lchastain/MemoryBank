pushd ..\src
javac -classpath ".;..\image4j.jar" -Xlint MemoryBank.java
jar cvf ..\mbank.jar *.class
call noclass
popd
