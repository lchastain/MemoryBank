# Data Locations

User data location when running via a jar file:  
[user home]/mbankData/[userEmail]/   
If this directory is not found, it will be created.  User data will go here.

user data location when running via the IDE:  
[your development folder]/mbDevData/[userEmail]  
Set the email address (ie, which data set to use) as a program option in the run configurations.
See the jondo configuration for an example.
If this directory is not found, it will be created.  User data will go here.

There is no access from development code to 'real' data because data that the user intends to keep 
needs to remain uncorrupted by testing and unnecessary, partial or nonsensical notes made during 
development.  If such an access need arises (in either direction) then the best 
advice is to make a renamed copy and put it where it can be 'seen' by the executing code, then 
adjust the run config or execution parameters to access it as a different/test user.

Icon location when running via the IDE:  
[your development folder]/target/classes/icons/
This is the location for icon selections via the IconFileChooser (a child of JFileChooser), as 
well as the location from where they are loaded when referenced by a note.

Icon location when running via a jar file:
For selections by the IconFileChooser:
[system temp directory]/membankResources/icons  
For access by a single note, the icon is taken directly from resources embedded in the jar file.   

Help files location when running via the IDE:
[your development folder]/src/main/resources/help
All markdown files, with some embedded .png file references
can start from README.md or TableOfContents.md

help files location when running via a jar file:
A.  .md and .html files
B.  .png embedded graphics  (images)


