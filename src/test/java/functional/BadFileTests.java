import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;

public class BadFileTests {

    // This class tests the exception handling for 'bad' data files.  It uses a 'resource' where the needed
    // files are actually instead directories, so that the operations attempted will fail.  In production,
    // these situations are handled and so no exception stacktraces are seen by the user or by this class.
    // But if you want to see them, uncomment the 'MemoryBank.debug' line.

    @BeforeAll
    static void meFirst() throws IOException {
        //MemoryBank.debug = true;

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        try {
            FileUtils.cleanDirectory(testData);
        } catch (Exception ignore){}

        // Retrieve a fresh set of test data from test resources
        String fileName = "leroy.brown@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);

        // Load up this Test user's application options
        AppOptions.loadOpts();
    }

    // The exceptions for these classes are caught and handled; we're just causing them now to get the coverage
    // but the tests do not need to 'see' the exceptions, and they will not, since they are being handled.

    // Note 2 Apr 2022 - no longer have 'xNoteDefaults' - make some new tests here, or throw this file out altogether.

//    @Test
//    void testDayNoteDefaults() {
//        DayNoteDefaults dayNoteDefaults = new DayNoteDefaults();
//        DayNoteDefaults.load();
//        dayNoteDefaults.save();
//    }
//
//    @Test
//    void testEventNoteDefaults() {
//        EventNoteDefaults eventNoteDefaults = new EventNoteDefaults();
//        EventNoteDefaults.load();
//        eventNoteDefaults.save();
//    }

}
