import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

class MemoryBankTest {

    @BeforeAll
    static void meFirst() throws IOException {
        MemoryBank.debug = true;

        // Set the locations for our user data (the directories will be created, if not already there)
        MemoryBank.setUserDataHome("null");
        File testBadData = new File(MemoryBank.userDataHome);
        MemoryBank.setUserDataHome("test.user@lcware.net");
        File testGoodData = new File(MemoryBank.userDataHome);

        // Remove any pre-existing Test data
        FileUtils.cleanDirectory(testBadData);
        FileUtils.cleanDirectory(testGoodData);

        // Retrieve a fresh set of test data from test resources
        File testBadResource = FileUtils.toFile(MemoryBank.class.getResource("null"));
        FileUtils.copyDirectory(testBadResource, testBadData);
        File testGoodResource = FileUtils.toFile(MemoryBank.class.getResource("jondo.nonamus@lcware.net"));
        FileUtils.copyDirectory(testGoodResource, testGoodData);

        TestUtil tu = new TestUtil();
        MemoryBank.optionPane = tu;
        MemoryBank.system = tu;
    }

    @BeforeEach
    void setup(){
        MemoryBank.setUserDataHome("test.user@lcware.net");
    }

    // The 'happy path' is covered by other tests;
    // this is for testing the Exception handling.
    @Test
    void testLoadOpts() {
        MemoryBank.setUserDataHome(null);
        MemoryBank.loadOpts(); // Outputs an expected stacktrace.
    }

    // Testing the improper usage.
    @Test
    void testTrace() {
        MemoryBank.init = true;
        MemoryBank.trace();
    }

    @Test
    void testMain() {
        MemoryBank.setUserDataHome("test.user@lcware.net");
        String[] theArgs = new String[]{"freddo", "-debug", "-event", "-trace", "-timing", "test.user@lcware.net"};
        MemoryBank.main(theArgs);
        MemoryBank.logFrame.dispatchEvent(new WindowEvent(MemoryBank.logFrame, WindowEvent.WINDOW_CLOSING));
    }

    // The 'happy path' is covered by other tests;
    // this is for testing the Exception handling.
    @Test
    void testSaveOpts() {
        MemoryBank.setUserDataHome(null);
        MemoryBank.saveOpts();
    }
}