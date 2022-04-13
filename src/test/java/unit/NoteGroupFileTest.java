import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertSame;

class NoteGroupFileTest {

    @BeforeAll
    static void setup() throws IOException {
        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve a fresh set of test data from test resources.
        // This test user has a rich set of known data, includes Search Results and Todo Lists
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        assert testResource != null;
        FileUtils.copyDirectory(testResource, testData);

        // Load up this Test user's application options
        AppOptions.loadOpts();
    }

    @Test
    void testGetGroupInfoFromFile() {
        GroupInfo groupInfo;

        // Test Calendar Note types
        //------------------------------------------------------------------------------
        File dataDir = new File(NoteGroupFile.calendarNoteGroupAreaPath + File.separatorChar + "2019");
        // This Calendar group repo has test data for groups of Day, Month, and Year.

        // Get the complete list of Group filenames.
        File[] theFiles = dataDir.listFiles();
        assert theFiles != null;
        for(File aFile: theFiles) {
            System.out.println(aFile.toString());
            groupInfo = NoteGroupFile.getGroupInfoFromFilePath(aFile);
            //System.out.println(groupInfo.toString());
            Assertions.assertTrue(groupInfo.getGroupName().endsWith("2019"));
        }

        // Test Event Note types
        //------------------------------------------------------------------------------
        dataDir = new File(NoteGroupFile.eventGroupAreaPath);

        // Get the complete list of Group filenames.
        theFiles = dataDir.listFiles();
        assert theFiles != null;
        for(File aFile: theFiles) {
            System.out.println(aFile.toString());
            groupInfo = NoteGroupFile.getGroupInfoFromFilePath(aFile);
            //System.out.println(groupInfo.toString());
            assertSame(groupInfo.groupType, GroupType.EVENTS);
        }

        // Test Goal Note types
        //------------------------------------------------------------------------------
        dataDir = new File(NoteGroupFile.goalGroupAreaPath);

        // Get the complete list of Group filenames.
        theFiles = dataDir.listFiles();
        assert theFiles != null;
        for(File aFile: theFiles) {
            System.out.println(aFile.toString());
            groupInfo = NoteGroupFile.getGroupInfoFromFilePath(aFile);
            //System.out.println(groupInfo.toString());
            assertSame(groupInfo.groupType, GroupType.GOALS);
        }

        // Test SearchResult types
        //------------------------------------------------------------------------------
        dataDir = new File(NoteGroupFile.searchResultGroupAreaPath);

        // Get the complete list of Group filenames.
        theFiles = dataDir.listFiles();
        assert theFiles != null;
        for(File aFile: theFiles) {
            System.out.println(aFile.toString());
            groupInfo = NoteGroupFile.getGroupInfoFromFilePath(aFile);
            //System.out.println(groupInfo.toString());
            assertSame(groupInfo.groupType, GroupType.SEARCH_RESULTS);
        }

        // Test TodoList types
        //------------------------------------------------------------------------------
        dataDir = new File(NoteGroupFile.todoListGroupAreaPath);

        // Get the complete list of Group filenames.
        theFiles = dataDir.listFiles();
        assert theFiles != null;
        for(File aFile: theFiles) {
            System.out.println(aFile.toString());
            groupInfo = NoteGroupFile.getGroupInfoFromFilePath(aFile);
            //System.out.println(groupInfo.toString());
            assertSame(groupInfo.groupType, GroupType.TODO_LIST);
        }
    }

    @Test
    void testGetNextDateWithData() {
        GroupInfo gi = new GroupInfo("x", GroupType.DAY_NOTES); // Name does not matter; we will not load.
        NoteGroupDataAccessor ngda = MemoryBank.dataAccessor.getNoteGroupDataAccessor(gi);
        LocalDate startDate = LocalDate.of(2019, 9, 2);
        LocalDate targetDate;

        targetDate = ngda.getNextDateWithData(startDate, ChronoUnit.DAYS, CalendarNoteGroup.Direction.FORWARD);
        System.out.println("From a starting date of " + startDate);
        System.out.println("  The Next forward date with data is: " + targetDate);

        targetDate = ngda.getNextDateWithData(startDate, ChronoUnit.DAYS, CalendarNoteGroup.Direction.BACKWARD);
        System.out.println("  and the Next backward date with data is: " + targetDate);
    }

    }