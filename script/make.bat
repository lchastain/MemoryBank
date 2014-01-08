pushd ..\src
javac -Xlint MemoryBank.java
jar cvf ..\mbank.jar *.class
call noclass
popd
