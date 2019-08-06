Test development to this point (8/2019) has been minimal and sporadic.

What is seen here are a few files files (in both .doc and .txt format) that describe the manual steps to be 
taken in order to test some of the interfaces.  

The TreeBranchEditorExample.java file in TestDrivers provides a different/separate app that can be used for
testing, that does not add to Memory Bank's normal operating codebase or affect 'real' data.  Instructions
for using it are here in the TreeBranchEditor.txt file.

But now, automated junit tests are being added to the
project.  The goal is to eventually convert everything here over to that type of test, remove the
files and folder here as well as the 'TestDrivers' folder and file, and instead document the new tests
in a more industry-standard way.

To generate a test file for a class that does not yet have one:
  In IntelliJ, with the class open - menu Navigation, select 'Test' and generate a new file from there.
 	Note that the Test option goes away once the Test is created.


