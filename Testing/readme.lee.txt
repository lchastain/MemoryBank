Test development to this point (8/2019) has been minimal and sporadic.

What is seen here are a few files files (in both .doc and .txt format) that describe the manual steps to be 
taken in order to test some of the interfaces.  These cannot be accomplished by Unit Tests.

The TreeBranchEditorExample.java file in TestDrivers provides an app that can be used for
testing the TreeBranchEditor.  Instructions
for using it are here in the TreeBranchEditor.txt file.

Now, junit tests are being added to the project.  But the tests that are here in this area
are functional in nature and not well replaced by unit tests.

The files and folders here and in the 'TestDrivers' folder need to be ported into a new functional
test area.

Update 8/21/2019 - Previously had trouble getting sub-directories of tests to work the way I wanted,
but today discovered that it's all controlled by how the project structure defines Test Sources.  Was
able to make the sub-directory by first removing test sources, then make the dir, then move the test
classes into there, then set the new dir as Test Sources.  It did not require a 'package' change.  So
now, can have separate areas for Unit tests vs Functional tests.  You get further control with the
Run configurations, to run either All or one, and if you want a specific grouping then use the Suite
capability and make a Run configuration for that one suite class.  The possibility of Suites makes
the 'run all' option less-and-less attractive, and as more tests of different flavors come on line,
running ALL tests will become counter-productive and altogether undesirable.

Note that tests are run either command-line java, or via Intellij.  There is no 'mvn test' capability
because the project is not (yet?) mavenized.

It took a while to understand that JUnit5 test suites are just the embedded (vintage) backwards
compatibility for JUnit4; you cannot do away with the @RunWith, but you do need to change which
library provides it, and IntelliJ will always first suggests the earlier libs so that's a
problem if you don't already know what you need.  Hours of internet research and I got it working
but couldn't say exactly how; many new libraries were downloaded and referenced.