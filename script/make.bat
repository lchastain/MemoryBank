pushd ..\src\main\java
"C:\Program Files\Java\jdk-16.0.2\bin\javac.exe" -classpath ".;c:\jars\*" -Xlint MemoryBank.java
jar cvf ..\..\..\mbank.jar *.class ..\..\..\images ..\..\..\icons
call noclass
popd
