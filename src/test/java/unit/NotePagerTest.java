import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

// The NotePager is built into the base NoteGroup class, but only some child classes
// use it.  For these tests we will use a TodoNoteGroup as the driver for the NotePager.

// But - the methods that need unit testing beyond what we already get for 'free' by
//     testing the full NoteGroup classes - are all event handlers.  Luckily, we can
//     make these events here 'from scratch', using the NotePager component that is
//     constructed by the base NoteGroup.

// There are three constructors for MouseEvent:
//   MouseEvent(Component source, int id, long when, int modifiers, int x, int y, int clickCount, boolean popupTrigger)
//   MouseEvent(Component source, int id, long when, int modifiers, int x, int y, int clickCount, boolean popupTrigger, int button)
//   MouseEvent(Component source, int id, long when, int modifiers, int x, int y, int xAbs, int yAbs, int clickCount, boolean popupTrigger, int button)


class NotePagerTest {
    TodoNoteGroupPanel todoNoteGroup;

    @BeforeAll
    static void meFirst() throws IOException {
        MemoryBank.debug = true;

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        try {
            FileUtils.cleanDirectory(testData);
        } catch (Exception ignore){}

        // Retrieve fresh test data from test resources.
        // We don't want a full set of data for these tests; just two Todo lists.
        String fileName = "todo_Long List.json";
        File newname = new File(NoteGroupFile.todoListGroupAreaPath + fileName);
        File testFile = FileUtils.toFile(TodoNoteGroupPanel.class.getResource("NotePagerTest" + File.separatorChar + fileName));
        FileUtils.copyFile(testFile, newname);

        fileName = "todo_PageRollover.json";
        newname = new File(NoteGroupFile.todoListGroupAreaPath + fileName);
        testFile = FileUtils.toFile(TodoNoteGroupPanel.class.getResource("NotePagerTest" + File.separatorChar + fileName));
        FileUtils.copyFile(testFile, newname);
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    // This test is only for coverage; not a lot else going on there; no further verification needed.
    @Test
    void testMouseMoves() {
        todoNoteGroup = new TodoNoteGroupPanel(new GroupInfo("Long List", GroupType.TODO_LIST), 10);
        NotePager notePager = todoNoteGroup.theNotePager;
        LabelButton leftAb = (LabelButton) notePager.getComponent(0);
        LabelButton middle = (LabelButton) notePager.getComponent(1);
        LabelButton rightAb = (LabelButton) notePager.getComponent(3);
        LabelButton defaultCase = new LabelButton("default");

        MouseEvent me1 = new MouseEvent(leftAb, MouseEvent.MOUSE_ENTERED, 0, 0, 18, 23, 0, false);
        MouseEvent me2 = new MouseEvent(middle, MouseEvent.MOUSE_ENTERED, 0, 0, 18, 23, 0, false);
        MouseEvent me3 = new MouseEvent(rightAb, MouseEvent.MOUSE_ENTERED, 0, 0, 18, 23, 0, false);
        MouseEvent me4 = new MouseEvent(defaultCase, MouseEvent.MOUSE_ENTERED, 0, 0, 18, 23, 0, false);

        notePager.mouseEntered(me1);
        notePager.mouseExited(me1);
        notePager.mouseEntered(me2);
        notePager.mouseEntered(me3);
        notePager.mouseEntered(me4);
    }

    @Test
    void testMouseActions() {
        todoNoteGroup = new TodoNoteGroupPanel(new GroupInfo("Long List", GroupType.TODO_LIST), 10);
        NotePager notePager = todoNoteGroup.theNotePager;
        LabelButton leftAb = (LabelButton) notePager.getComponent(0);
        LabelButton middle = (LabelButton) notePager.getComponent(1);
        LabelButton rightAb = (LabelButton) notePager.getComponent(3);

        MouseEvent me1 = new MouseEvent(rightAb, MouseEvent.MOUSE_PRESSED, 0, 0, 18, 23, 0, false);
        notePager.mousePressed(me1);  // takes us to page 2 of 3
        notePager.mousePressed(me1);  // takes us to page 3 of 3
        notePager.mousePressed(me1);  // Back to where we started, and tests the wrap-around.

        MouseEvent me2 = new MouseEvent(leftAb, MouseEvent.MOUSE_PRESSED, 0, 0, 18, 23, 0, false);
        notePager.mousePressed(me2);  // takes us to page 3 of 3, and tests the wrap-around.
        notePager.mousePressed(me2);  // takes us to page 2 of 3
        notePager.mousePressed(me2);  // takes us to page 1 of 3

        MouseEvent me3 = new MouseEvent(middle, MouseEvent.MOUSE_PRESSED, 0, 0, 18, 23, 0, false);
        notePager.mousePressed(me3);
        notePager.mouseReleased(me3);
        notePager.mouseClicked(me3);
    }

    @Test
    void testGetHighestPage() {
        todoNoteGroup = new TodoNoteGroupPanel(new GroupInfo("Long List", GroupType.TODO_LIST), 10);
        int numPages = todoNoteGroup.theNotePager.getHighestPage();
        Assertions.assertEquals(3, numPages);
    }

}