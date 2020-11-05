import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class NoteGroupFileTest {

    @BeforeAll
    static void setup() throws IOException {
        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve a fresh set of test data from test resources.
        // This test user has a rich set of known data, includes Search Results and Todo Lists
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);

        // Load up this Test user's application options
        AppOptions.loadOpts();
    }


    @BeforeEach
    void setUp() {
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
        for(File aFile: theFiles) {
            System.out.println(aFile.toString());
            groupInfo = NoteGroupFile.getGroupInfoFromFile(aFile);
            //System.out.println(groupInfo.toString());
            Assertions.assertTrue(groupInfo.getGroupName().endsWith("2019"));
        }

        // Test Event Note types
        //------------------------------------------------------------------------------
        dataDir = new File(NoteGroupFile.eventGroupAreaPath);

        // Get the complete list of Group filenames.
        theFiles = dataDir.listFiles();
        for(File aFile: theFiles) {
            System.out.println(aFile.toString());
            groupInfo = NoteGroupFile.getGroupInfoFromFile(aFile);
            //System.out.println(groupInfo.toString());
            assertSame(groupInfo.groupType, GroupInfo.GroupType.EVENTS);
        }

        // Test Goal Note types
        //------------------------------------------------------------------------------
        dataDir = new File(NoteGroupFile.goalGroupAreaPath);

        // Get the complete list of Group filenames.
        theFiles = dataDir.listFiles();
        for(File aFile: theFiles) {
            System.out.println(aFile.toString());
            groupInfo = NoteGroupFile.getGroupInfoFromFile(aFile);
            //System.out.println(groupInfo.toString());
            assertSame(groupInfo.groupType, GroupInfo.GroupType.GOALS);
        }

        // Test SearchResult types
        //------------------------------------------------------------------------------
        dataDir = new File(NoteGroupFile.searchResultGroupAreaPath);

        // Get the complete list of Group filenames.
        theFiles = dataDir.listFiles();
        for(File aFile: theFiles) {
            System.out.println(aFile.toString());
            groupInfo = NoteGroupFile.getGroupInfoFromFile(aFile);
            //System.out.println(groupInfo.toString());
            assertSame(groupInfo.groupType, GroupInfo.GroupType.SEARCH_RESULTS);
        }

        // Test TodoList types
        //------------------------------------------------------------------------------
        dataDir = new File(NoteGroupFile.todoListGroupAreaPath);

        // Get the complete list of Group filenames.
        theFiles = dataDir.listFiles();
        for(File aFile: theFiles) {
            System.out.println(aFile.toString());
            groupInfo = NoteGroupFile.getGroupInfoFromFile(aFile);
            //System.out.println(groupInfo.toString());
            assertSame(groupInfo.groupType, GroupInfo.GroupType.TODO_LIST);
        }
    }
}