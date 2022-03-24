import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

// This tests the two default methods of the LinkHolder interface, and the two known implementations of its
//   one required member method.

// Currently there are 6 different group and note types, for a total of 12 possible source entities for a link.
//   If you then consider that each source type could link to any one of the others, including its own type, then
//   the total number of link types is 12x12 = 144.  Each of those of course will have its reverse.
// But the fact is that there are only two implementations of 'createReverseLink()', and neither of those has a
//   branch that forks off due to variations of data type.
// So the point is - yes, there are all different link types but it would be inefficient to test them all from the
//   perspective of their reverse links; only two tests are needed for coverage of reverse link creation, and all
//   the other considerations for different data types will still be covered by the unit tests for each of those types
//   as well as the functional tests where those various group types are loaded, manipulated and persisted.

class ReverseLinkagesTest {
    static AppTreePanel appTreePanel;

    @BeforeAll
    static void setup() throws IOException {
        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve a fresh set of test data from test resources.
        // This test user six pre-established NoteGroups, one for each type of Goal, Event, TodoList, and Day/Month/Year.
        // These groups each have six notes, one for each type that they might be linked to.  One or more tests in this
        // class is expecting these notes to be present, so that the test can set links on them.
        String fileName = "setup@testing.com";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);

        // Load up this Test user's application options
        AppOptions.loadOpts();

        appTreePanel = new AppTreePanel(new JFrame("Reverse Linkages Test"), MemoryBank.appOpts);
    }

    @AfterAll
    static void tearDown() {
        appTreePanel = null; // allow GC to get rid of this before other test threads see the instance.
        System.gc();
    }


    @Test
    void testAddReverseLinksToTodoNote() {
        // Create two different groups that will be used as link targets, below.
        GoalGroupPanel goalGroupPanel = new GoalGroupPanel("Take Over The World");
        DayNoteGroupPanel dayNoteGroupPanel = new DayNoteGroupPanel(); // Unspecified date means that it will be today

        // Put a note into the GoalGroup (and save it)
        Vector<NoteData> goalVector = new Vector<>(0, 1);
        TodoNoteData milestoneData;  // The type here might change, eventually.  Shouldn't be a TodoNoteData, but goal-something.
        milestoneData = new TodoNoteData();
        milestoneData.noteString = "milestone note.";
        goalVector.add(milestoneData);
        goalGroupPanel.myNoteGroup.setNotes(goalVector);
        goalGroupPanel.myNoteGroup.saveNoteGroup();

        // The differences here between Goal saving (above) and Day group saving - is to go two different paths -
        // Goal saves here and now via the Accessor method, and for DayNoteGroup, during the test
        //   we just put the changed group into its keeper and then before adding the reverse link,
        //   getNoteGroupDataAccessor calls refresh(), to save the group at that time.

        // Put a note into the DayNote Group (it gets saved later)
        NoteData dayNoteData = new DayNoteData();
        dayNoteData.noteString = "day note";
        dayNoteGroupPanel.myNoteGroup.appendNote(dayNoteData);
        dayNoteGroupPanel.myNoteGroup.setNotes(dayNoteGroupPanel.myNoteGroup.getTheData()[1]);
        dayNoteGroupPanel.loadPage(1);

        // And these two lines are needed because these groups will not actually be found in 'real' data,
        //   so we inject them directly to the AppTreePanel, where it will appear that they have already
        //   been loaded.  Note that after this test runs, the groups WILL be in the (test) data.
        appTreePanel.theGoalsKeeper.add(goalGroupPanel);
        appTreePanel.theAppDays = dayNoteGroupPanel;

        // Make a new Todo note and add it to a (fake new) TodoNoteGroup.
        // This note will be the source entity for the reverse links.
        // For reverse link creation we need to add the note to a 'real' group, but that group does not
        //   need to be persisted before (or after) we add the reverse link.  This is because this test
        //   is about links going to the targets; it doesn't need the source entity to be 'real' beyond
        //   the needs of the code under test.
        TodoNoteData todoNoteData = new TodoNoteData();
        todoNoteData.noteString = "Links from a TodoNote.";
        NoteGroupPanel todoNoteGroupPanel = new TodoNoteGroupPanel("Nada");
        todoNoteGroupPanel.myNoteGroup.appendNote(todoNoteData);

        // Create two (valid) links.  We want the loop for adding a reverse link to run twice.
        // Remember that the owner/source of the links is not held in the link data itself, but
        //   is inferred from where the LinkedEntityData is held.
        GroupInfo goalGroupInfo = new GroupInfo(goalGroupPanel.myNoteGroup.getGroupProperties());
        NoteInfo goalNoteInfo = new NoteInfo(milestoneData);
        LinkedEntityData led1 = new LinkedEntityData(goalGroupInfo, goalNoteInfo);
        led1.linkType = LinkedEntityData.LinkType.DEPENDING_ON;
        GroupInfo dayNoteGroupInfo = new GroupInfo(dayNoteGroupPanel.myNoteGroup.getGroupProperties());
        NoteInfo dayNoteInfo = new NoteInfo(dayNoteData);
        LinkedEntityData led2 = new LinkedEntityData(dayNoteGroupInfo, dayNoteInfo);
        led2.linkType = LinkedEntityData.LinkType.AFTER;

        // Put the two links into a LinkTargets collection that we can send to the method.
        LinkTargets linkTargets = new LinkTargets();
        linkTargets.add(led1);
        linkTargets.add(led2);
        // We don't need to actually set the todoNoteData's linkTargets in order to test the reverse link creation.

        // Here it is; not
        todoNoteData.addReverseLinks(linkTargets);
    }

    // In this case the original links are to Groups and not notes within those groups.
    @Test
    void testAddReverseLinksToTodoNoteGroup() {
        // Create two different link-target groups.  These two ARE in the test data; no need to bother the AppTreePanel.
        TodoNoteGroupPanel todoNoteGroupPanel = new TodoNoteGroupPanel("Preparations");
        EventNoteGroupPanel eventNoteGroupPanel = new EventNoteGroupPanel("Reverse Links");

        // Make a NoteGroup that will be the source entity for the reverse links.
        // For this test the source could have also been a Note.  This way we have a bit more variety
        //   in the setups but as already stated, that probably doesn't matter in these test cases.
        NoteGroupPanel monthNoteGroupPanel = new MonthNoteGroupPanel();

        // Create two (valid) links.  We want the loop for adding a reverse link to run twice.
        // Remember that the owner/source of the links is not held in the link data itself, but
        //   is inferred from where the LinkedEntityData is held.
        GroupInfo eventGroupInfo = new GroupInfo(eventNoteGroupPanel.myNoteGroup.getGroupProperties());
        LinkedEntityData led1 = new LinkedEntityData(eventGroupInfo, null);
        led1.linkType = LinkedEntityData.LinkType.DEPENDING_ON;
        GroupInfo todoGroupInfo = new GroupInfo(todoNoteGroupPanel.myNoteGroup.getGroupProperties());
        LinkedEntityData led2 = new LinkedEntityData(todoGroupInfo, null);
        led2.linkType = LinkedEntityData.LinkType.AFTER;

        // Put the two links into a LinkTargets collection that we can send to the method.
        LinkTargets linkTargets = new LinkTargets();
        linkTargets.add(led1);
        linkTargets.add(led2);
        //monthNoteGroupPanel.linkTargets = linkTargets; // This is for completeness; wasn't necessary for this test.

        // Here it is -
        monthNoteGroupPanel.myNoteGroup.addReverseLinks(linkTargets);
    }

}