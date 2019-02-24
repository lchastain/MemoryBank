pushd ..\src
"C:\Program Files\Java\jdk1.8.0_192\bin\javac.exe" -classpath ".;c:\jars\*" -Xlint MemoryBank.java
jar cvf ..\mbank.jar *.class
call noclass
popd
