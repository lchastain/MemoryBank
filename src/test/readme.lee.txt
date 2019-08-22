
At this writing the application is not (yet?) broken up into modules, so true 'integration' testing is
not really relevant and therefore neither would be 'end to end', by the strict definition.  But still
there are a lot of moving parts in this app between program startup and any one functionality, so if a
capability can be tested by accessing public method(s) without running the entire app to get to it then
we'll call it a functional test.  And if a test requires you to start at the 'front door' by running
the entire app then we'll call it an end-to-end test.


Test classes and suites annotated with @RunWith(JUnitPlatform.class) cannot be executed directly on the JUnit Platform
(or as a "JUnit 5" test as documented in some IDEs). Such classes and suites can only be executed using JUnit 4 infrastructure.