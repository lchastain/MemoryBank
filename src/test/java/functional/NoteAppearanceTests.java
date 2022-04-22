import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

// This class tests the following situations:
// 1.  NoteGroups load a finite amount of data, and the rest of the interface is 'empty' (not visible).
// 2.  Prior to any change, the NoteGroup has 'undo' and 'save' menu items that are disabled.
// 3.  After a change, those menu items are enabled.
// 4.  An 'undo' will restore the data in the interface to its original form, and disable the menu item.
// 5.  A 'save' will save the data, refresh the interface, and disable the menu item.

// Where to look:
// NoteGroup - setListMenu, adjustMenuItems

// Not (yet?) tested:
//--------------------
// Component shift up/down
// Loading a group smaller than the one it just had
// Merging
// Gap elimination


public class NoteAppearanceTests {
    static AppMenuBar appMenuBar;

    @BeforeAll
    static void setup() throws IOException {
        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        try {
            FileUtils.cleanDirectory(testData);
        } catch (Exception ignore){}

        // Retrieve the test data for this class from test resources.
        String fileName = "NoteAppearanceTest";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        assert testResource != null;
        FileUtils.copyDirectory(testResource, testData);

        // Needed ancillary class
        appMenuBar = new AppMenuBar();
    }

    // The two Event groups in the test data have three and five notes, respectively.
    @Test
    void testEvents() {
        EventNoteGroupPanel eventNoteGroup = new EventNoteGroupPanel("One");

        // Test condition 1:  NoteGroup loads the data, and the rest of the interface is 'empty' (not visible).
        //-------------------------------------------------------------------------------------------------------------
        // The last data item (zero-based indexing)
        EventNoteComponent eventNoteComponent2 = (EventNoteComponent) eventNoteGroup.getNoteComponent(2);
        Assertions.assertTrue(eventNoteComponent2.isVisible());
        Assertions.assertTrue(eventNoteComponent2.initialized);

        // The last visible line, empty and not initialized
        EventNoteComponent eventNoteComponent3 = (EventNoteComponent) eventNoteGroup.getNoteComponent(3);
        Assertions.assertTrue(eventNoteComponent3.isVisible());
        Assertions.assertFalse(eventNoteComponent3.initialized);

        // The first non-visible line, empty and not initialized
        EventNoteComponent eventNoteComponent4 = (EventNoteComponent) eventNoteGroup.getNoteComponent(4);
        Assertions.assertFalse(eventNoteComponent4.isVisible());
        Assertions.assertFalse(eventNoteComponent4.initialized);
        //-------------------------------------------------------------------------------------------------------------

        // Test condition 2:  Disabled menu items
        JMenu theMenu = AppMenuBar.getNodeMenu("Upcoming Event");
        eventNoteGroup.setListMenu(theMenu);
        JMenuItem theUndo = AppUtil.getMenuItem(theMenu, "Undo All");
        Assertions.assertFalse(theUndo.isEnabled());
        JMenuItem theSave = AppUtil.getMenuItem(theMenu, "Save");
        Assertions.assertFalse(theSave.isEnabled());

        // Test condition 3:  Menu items enabled after a change
        NoteData newNoteData = new NoteData();
        newNoteData.setNoteString("A new note!");
        eventNoteComponent3.setNoteData(newNoteData);
        Assertions.assertTrue(theUndo.isEnabled());
        Assertions.assertTrue(theSave.isEnabled());

        // Test condition 4:  An 'undo' will restore the interface and disable the menu item.
        //-------------------------------------------------------------------------------------------------------------
        // We cannot just click the menu item here; normally the AppTreePanel would handle that event.
        // Instead, we do what the handler would do:
        eventNoteGroup.updateGroup();

        // And now, check to see that the info we added in condition 3, is now gone again.
        NoteData noteData = eventNoteComponent3.getNoteData();
        Assertions.assertTrue(noteData.noteString.isEmpty());

        // And the menu item is back to disabled.
        Assertions.assertFalse(theUndo.isEnabled());
        //-------------------------------------------------------------------------------------------------------------

        // Test condition 5:  Save the data, refresh the interface, and disable the menu item.
        //-------------------------------------------------------------------------------------------------------------
        // First, repeat some of the earlier work, to get back to a 'group changed' state.
        eventNoteComponent3.setNoteData(newNoteData);
        Assertions.assertTrue(theSave.isEnabled());

        // This is what the 'save' menu item does, when clicked.
        eventNoteGroup.refresh();

        // Check the interface - one new item in the list.
        // Now component 4 looks like 3 did, earlier.
        Assertions.assertTrue(eventNoteComponent4.isVisible());
        Assertions.assertFalse(eventNoteComponent4.initialized);

        // And the menu item is back to disabled.
        Assertions.assertFalse(theSave.isEnabled());
        //-------------------------------------------------------------------------------------------------------------
    }

    // The two Todo Lists in the test data have three and five notes, respectively.
    @Test
    void testTodoLists() {
        TodoNoteGroupPanel todoNoteGroup = new TodoNoteGroupPanel("one");

        // Test condition 1:  NoteGroup loads a finite amount of data, and the rest of the interface is 'empty' (not visible).
        //-------------------------------------------------------------------------------------------------------------
        // The last data item is visible and initialized (zero-based indexing)
        TodoNoteComponent todoNoteComponent2 = todoNoteGroup.getNoteComponent(2);
        Assertions.assertTrue(todoNoteComponent2.isVisible());
        Assertions.assertTrue(todoNoteComponent2.initialized);

        // The last visible line, empty and not initialized
        TodoNoteComponent todoNoteComponent3 = todoNoteGroup.getNoteComponent(3);
        Assertions.assertTrue(todoNoteComponent3.isVisible());
        Assertions.assertFalse(todoNoteComponent3.initialized);

        // The first non-visible line, empty and not initialized
        TodoNoteComponent todoNoteComponent4 = todoNoteGroup.getNoteComponent(4);
        Assertions.assertFalse(todoNoteComponent4.isVisible());
        Assertions.assertFalse(todoNoteComponent4.initialized);
        //-------------------------------------------------------------------------------------------------------------

        // Test condition 2:  Disabled menu items
        JMenu theMenu = AppMenuBar.getNodeMenu("To Do List");
        todoNoteGroup.setListMenu(theMenu);
        JMenuItem theUndo = AppUtil.getMenuItem(theMenu, "Undo All");
        Assertions.assertFalse(theUndo.isEnabled());
        JMenuItem theSave = AppUtil.getMenuItem(theMenu, "Save");
        Assertions.assertFalse(theSave.isEnabled());

        // Test condition 3:  Menu items enabled after a change
        NoteData newNoteData = new NoteData();
        newNoteData.setNoteString("A new note!");
        todoNoteComponent3.setNoteData(newNoteData);
        Assertions.assertTrue(theUndo.isEnabled());
        Assertions.assertTrue(theSave.isEnabled());

        // Test condition 4:  An 'undo' will restore the interface and disable the menu item.
        //-------------------------------------------------------------------------------------------------------------
        // We cannot just click the menu item here; normally the AppTreePanel would handle that event.
        // Instead, we do what the handler would do:
        todoNoteGroup.updateGroup();

        // And now, check to see that the info we added in condition 3, is now gone again.
        NoteData noteData = todoNoteComponent3.getNoteData();
        Assertions.assertTrue(noteData.noteString.isEmpty());

        // And the menu item is back to disabled.
        Assertions.assertFalse(theUndo.isEnabled());
        //-------------------------------------------------------------------------------------------------------------

        // Test condition 5:  Save the data, refresh the interface, and disable the menu item.
        //-------------------------------------------------------------------------------------------------------------
        // First, repeat some of the earlier work, to get back to a 'group changed' state.
        todoNoteComponent3.setNoteData(newNoteData);
        Assertions.assertTrue(theSave.isEnabled());

        // This is what the 'save' menu item does, when clicked.
        todoNoteGroup.refresh();

        // Check the interface - one new item in the list.
        // Now component 4 looks like 3 did, earlier.
        Assertions.assertTrue(todoNoteComponent4.isVisible());
        Assertions.assertFalse(todoNoteComponent4.initialized);

        // And the menu item is back to disabled.
        Assertions.assertFalse(theSave.isEnabled());
        //-------------------------------------------------------------------------------------------------------------

    }
}
