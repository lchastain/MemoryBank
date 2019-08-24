import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

class ClearNoteGroupTest {
    @BeforeAll
    static void setDataLocation() {
        MemoryBank.setUserDataHome("test.user@lcware.net");
    }

    // Test that clearing an empty Group will not cause errors and that the group remains empty.
    @Test
    void testClearEmptyNoteGroup() {
        MemoryBank.tempCalendar.setTime(new Date());
        int thisMonth = MemoryBank.tempCalendar.get(Calendar.MONTH);
        MonthNoteGroup mng = new MonthNoteGroup();
        mng.clearGroup();
        MemoryBank.tempCalendar.setTime(mng.getChoice());
        int thatMonth = MemoryBank.tempCalendar.get(Calendar.MONTH);
        Assertions.assertEquals(thisMonth, thatMonth);
        Vector<NoteData> theInfo = mng.getCondensedInfo();
        Assertions.assertEquals(0, theInfo.size());
    }

    // Test the clearing of an unsaved Group.
    @Test
    void testClearUnsavedNoteGroup() {
        // Initializations
        MonthNoteGroup mng = new MonthNoteGroup();
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
        mng.getNoteComponent(0).setNoteData(new NoteData(nd));
        nd.setNoteString(s2);
        mng.getNoteComponent(1).setNoteData(new NoteData(nd));
        nd.setNoteString(s3);
        mng.getNoteComponent(2).setNoteData(new NoteData(nd));

        // Next, verify that the data did 'take'
        Assertions.assertEquals(mnc1.getNoteData().noteString, s1);
        Assertions.assertEquals(mnc2.getNoteData().noteString, s2);
        Assertions.assertEquals(mnc3.getNoteData().noteString, s3);

        // Now clear it -
        mng.clearGroup();

        // And verify that it's 'gone' -
        // The clearing will have set the 'initialized' flag of the components, that controls whether getNoteData
        // returns the NoteData object or just a null.  In point of fact, components 2 and 3 do
        // still retain their NoteData objects, with the strings that were set above.  Component 1 gets immediate
        // visibility so its data is cleared first.  The others - when reused, will have theirs cleared before
        // going visible.
        Assertions.assertNull(mnc1.getNoteData());
        Assertions.assertNull(mnc2.getNoteData());
        Assertions.assertNull(mnc3.getNoteData());
        theInfo = mng.getCondensedInfo();
        Assertions.assertEquals(0, theInfo.size());
    }

    // Test the clearing of a saved Group.
    @Test
    void testClearSavedNoteGroup() {
        String strGroupFilename; // this can change; reacquire as needed.

        // Initializations
        MonthNoteGroup mng = new MonthNoteGroup();
        NoteData nd = new NoteData();
        String s1 = "Month clearing test line one";
        String s2 = "Month clearing test line two";
        String s3 = "Month clearing test line three";
        NoteComponent mnc1 = mng.getNoteComponent(0);
        NoteComponent mnc2 = mng.getNoteComponent(1);
        NoteComponent mnc3 = mng.getNoteComponent(2);

        // Ensure that initially, no file exists
        strGroupFilename = mng.getGroupFilename();
        File theFile = new File(strGroupFilename); // this works even when filename is empty.
        if (theFile.exists()) Assert.assertTrue(theFile.delete());

        // Set some data so that it may be cleared.
        // We use the copy-constructor of a new NoteData or else we would only
        // be setting the notestring of the same one in all three components.
        nd.setNoteString(s1);
        mng.getNoteComponent(0).initialize(); // Alternative to the keyTyped listener.
        mng.getNoteComponent(0).setNoteData(new NoteData(nd));

        nd.setNoteString(s2);
        mng.getNoteComponent(1).initialize();
        mng.getNoteComponent(1).setNoteData(new NoteData(nd));

        nd.setNoteString(s3);
        mng.getNoteComponent(2).initialize();
        mng.getNoteComponent(2).setNoteData(new NoteData(nd));

        // Now, verify that the data did get into the group.
        Assertions.assertEquals(mnc1.getNoteData().noteString, s1);
        Assertions.assertEquals(mnc2.getNoteData().noteString, s2);
        Assertions.assertEquals(mnc3.getNoteData().noteString, s3);

        // Now save the group
        mng.preClose();

        // Verify that there is now a file for it
        strGroupFilename = mng.getGroupFilename();
        Assertions.assertTrue(new File(strGroupFilename).exists());

        // Now clear the group -
        mng.clearGroup();

        // And verify that there is no longer a file for it
        Assertions.assertFalse(new File(strGroupFilename).exists());

        // And then verify that it's data is 'gone' from the group -
        // The clearing will have set the 'initialized' flag of the components, that controls whether getNoteData
        // returns the NoteData object or just a null.  In point of fact, components 2 and 3 do
        // still retain their NoteData objects, with the strings that were set above.  But component 1 gets immediate
        // visibility so its data really is cleared.  The others - when reused, will have theirs cleared before
        // going visible.  But here we are only concerned with the return value of getNoteData, not whether or
        // not the component's text field is still retaining the last string it contained.
        Assertions.assertNull(mnc1.getNoteData());
        Assertions.assertNull(mnc2.getNoteData());
        Assertions.assertNull(mnc3.getNoteData());
        Vector<NoteData> theInfo = mng.getCondensedInfo();
        Assertions.assertEquals(0, theInfo.size());
    }
}