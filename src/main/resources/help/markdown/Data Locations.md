# Data Locations

A unique user identifier is supplied as a program argument, in the form of an email address.
The user data developed by the application is then stored on the local filesystem under a
directory with that name.  If the directory is not found, it will be created.

When running via a jar file (production code), the filesystem location is:  
[user home]/mbankData/[userEmail]/   

When running via the IDE (development code), the filesystem location is:  
[your development folder]/mbDevData/[userEmail]  

Set the email address (ie, which data set to use) as a program option in the run configurations.
See the jondo configuration for an example.  

There is no crossover from development code to production data because data that the user intends 
to keep 
needs to remain uncorrupted by testing or unnecessary, partial or nonsensical notes made during 
development.  If such an access need arises (in either direction) then the best 
advice is to make a renamed copy and put it where it can be 'seen' by the executing code, then 
adjust the run config or execution parameters to access it as a different/test user.

---
Location of available icon selections presented by the IconFileChooser -  
When running via the IDE:  
[your development folder]/src/main/resources/icons/  
When running via a jar file:  
[system temp directory]/membankResources/icons/  

Location of the icon when displayed on either a notegroup line or on the MonthView -
the getResource method is used and the icon will come from the filesystem under 
src/main/resources when run via the ide, or from the jar's embedded resources when 
run via a jar file.

When a graphic needs to come from the images (vs the icons), the accesses and paths
are the same as they are leading up to the icons, but under the 'images' directory.

---
Help files location when running via the IDE:
[your development folder]/src/main/resources/help/markdown  

Help files location when running via a jar file:
[system temp directory]/membankResources/help/markdown


