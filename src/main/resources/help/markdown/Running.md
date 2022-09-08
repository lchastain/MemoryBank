# Running the Application

![](../../images/running.jpg)

As stated in the 'building' section, running via the ide is as simple as using
the provided run configuration, or cloning one with changes to make a new user.

Although the ide can construct an executable JAR, the external libraries are not
built into the jar and therefore cannot be found, so running the jar as an executable
file is not currently possible; you will need to run it by including it in the
classpath argument to the java executable, along with the external libraries, and 
specifying the main class, MemoryBank.
And when you do that, be sure to provide the user email program argument.  
Ex:  java -classpath [path to jar];[path to external libraries] MemoryBank jondo@lcware.net

Starting at C:\, here is the 'Target' from a windows shortcut that runs the app via the 
jar:  
"C:\Program Files\Java\jdk-16.0.2\bin\java.exe" -classpath 
[dev workspace]\out\artifacts\MemoryBank_jar\MemoryBank.jar;[path to external libraries] MemoryBank jondo@lcware.net

And similarly, to run the app via the jar built by Maven:
"C:\Program Files\Java\jdk-16.0.2\bin\java.exe" -classpath
[dev workspace]\target\MemoryBank-1.0-SNAPSHOT.jar;[path to external libraries] MemoryBank jondo@lcware.net


