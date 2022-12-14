import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Vector;

// For SCR0038

class ClearNoteGroupPanelTest {
    @BeforeAll
    static void setDataLocation() {
        MemoryBank.userEmail = "test.user@lcware.net";
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
        TestUtil.getTheAppTreePanel();
    }

    @Test
    // Test that clearing an empty Group will not cause errors and that the group remains empty.
    void testClearEmptyNoteGroup() {
        MonthNoteGroupPanel mng = new MonthNoteGroupPanel();

        // First, make sure we have an empty group -
        LocalDate theDate = LocalDate.of(1985,6,6);
        mng.setDate(theDate); // We don't expect any notes on this month for this user.
        Vector<NoteData> theInfo = mng.getCondensedInfo();
        Assertions.assertEquals(0, theInfo.size());
        // Ok, that was the setup; now run the test.

        mng.clearAllNotes();
        Assertions.assertEquals(theDate, mng.getDate());
        theInfo = mng.getCondensedInfo();
        Assertions.assertEquals(0, theInfo.size());
    }

    // Test the clearing of an unsaved Group.
    @Test
    void testClearUnsavedNoteGroup() {
        // Initializations
        MonthNoteGroupPanel mng = new MonthNoteGroupPanel();
        Vector<NoteData> theInfo;
        NoteData nd = new NoteData();
        String s1 = "Month clearing test line one";
        String s2 = "Month clearing test line two";
        String s3 = "Month clearing test line three";
        NoteComponent mnc1 = mng.getNoteComponent(0);
        NoteComponent mnc2 = mng.getNoteComponent(1);
        NoteComponent mnc3 = mng.getNoteComponent(2);

        // First, set some data so that it may be cleared.
        // We use the copy-constructor of a new NoteData or else we would only
        // be setting the notestring of the same one in all three components.
        nd.setNoteString(s1);
        mng.getNoteComponent(0).initialize(); // Alternative to the keyTyped listener.
        mng.getNoteComponent(0).setNoteData(new NoteData(nd));
        nd.setNoteString(s2);
        mng.getNoteComponent(1).initialize(); // These 'initialize' calls increase the lastVisibleNoteIndex.
        mng.getNoteComponent(1).setNoteData(new NoteData(nd));
        nd.setNoteString(s3);
        mng.getNoteComponent(2).initialize(); // And only the visible notes will be cleared.
        mng.getNoteComponent(2).setNoteData(new NoteData(nd));

        // Next, verify that the data did 'take'
        Assertions.assertEquals(mnc1.getNoteData().noteString, s1);
        Assertions.assertEquals(mnc2.getNoteData().noteString, s2);
        Assertions.assertEquals(mnc3.getNoteData().noteString, s3);

        // Now clear it -
        mng.clearAllNotes();
        // But be aware that each component may still have a data member.

        // And then verify that it's data is 'gone' from the group -
        // The clearing of the group will have cleared the 'initialized' flag of each of the components.
        Assertions.assertFalse(mnc1.initialized);
        Assertions.assertFalse(mnc2.initialized);
        Assertions.assertFalse(mnc3.initialized);
        theInfo = mng.getCondensedInfo();
        Assertions.assertEquals(0, theInfo.size());
    }

    // Test the clearing of a saved Group.
//    @Test
//    void testClearSavedNoteGroup() {
//        String strGroupFilename; // this can change; reacquire as needed.
//
//        // Initializations
//        MonthNoteGroupPanel mng = new MonthNoteGroupPanel();
//        NoteData nd = new NoteData();
//        String s1 = "Month clearing test line one";
//        String s2 = "Month clearing test line two";
//        String s3 = "Month clearing test line three";
//        NoteComponent mnc1 = mng.getNoteComponent(0);
//        NoteComponent mnc2 = mng.getNoteComponent(1);
//        NoteComponent mnc3 = mng.getNoteComponent(2);
//
//        // Ensure that initially, no file exists
//        strGroupFilename = mng.getGroupFilename();
//        File theFile = new File(strGroupFilename); // this works even when filename is empty.
//        if (theFile.exists()) Assert.assertTrue(theFile.delete());
//
//        // Set some data so that it may be cleared.
//        // We use the copy-constructor of a new NoteData or else we would only
//        // be setting the notestring of the same one in all three components.
//        nd.setNoteString(s1);
//        mng.getNoteComponent(0).initialize(); // Alternative to the keyTyped listener.
//        mng.getNoteComponent(0).setNoteData(new NoteData(nd));
//
//        nd.setNoteString(s2);
//        mng.getNoteComponent(1).initialize();
//        mng.getNoteComponent(1).setNoteData(new NoteData(nd));
//
//        nd.setNoteString(s3);
//        mng.getNoteComponent(2).initialize();
//        mng.getNoteComponent(2).setNoteData(new NoteData(nd));
//
//        // Now, verify that the data did get into the group.
//        Assertions.assertEquals(mnc1.getNoteData().noteString, s1);
//        Assertions.assertEquals(mnc2.getNoteData().noteString, s2);
//        Assertions.assertEquals(mnc3.getNoteData().noteString, s3);
//
//        // Now save the group
//        mng.preClosePanel();
//
//        // Verify that there is now a file for it
//        strGroupFilename = mng.getGroupFilename();
//        Assertions.assertTrue(new File(strGroupFilename).exists());
//
//        // Now clear the group -
//        mng.clearAllNotes();  // Clears the interface and the Vector for it.
//        // But be aware that each component may still have a data member.
//
//// This changed with the addition of Properties onto Notes - now the Properties causes a save, even without data.
////  This test can get along without the check for a missing file; all else still valid.
////        // And 'save' it again -
////        mng.preClosePanel(); // With an empty group, a save == a delete.
////
////        // And verify that there is no longer a file for it
////        Assertions.assertFalse(new File(strGroupFilename).exists());
//
//        // And then verify that it's data is 'gone' from the group -
//        // The clearing of the group will have cleared the 'initialized' flag of each of the components.
//        Assertions.assertFalse(mnc1.initialized);
//        Assertions.assertFalse(mnc2.initialized);
//        Assertions.assertFalse(mnc3.initialized);
//        Vector<NoteData> theInfo = mng.getCondensedInfo();
//        Assertions.assertEquals(0, theInfo.size());
//    }
}