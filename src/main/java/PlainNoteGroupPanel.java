import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

// A PlainNoteGroupPanel does not have its own specialized NoteGroup, NoteComponent or NoteData - it utilizes only
//   the base classes of these entities, hence the name 'plain'.

// A PlainNoteGroupPanel can either be a standalone Panel or it can be associated to a 'parent' panel.
// An association is defined via its name by prefixing 'notes_' to the name of the Group for the parent panel, and the
// data for it will go to the same location as the data for the parent panel.  For example the Notes for the Goal
// 'Retire' will be stored in the same location as the Goal and the name will be 'notes_Retire' which will have the
// effect of attaching it to the goal_Retire.

// Note:  with a new approach being developed for user notes, this panel is not currently (as of 25 Oct 2022) being
//   instantiated by the main MB application, which could allow NoteGroup to go back to being an abstract class.
//   But hold off on that until you are sure that you want to delete this class, or remove this comment if/when it
//   comes back into usage.

public class PlainNoteGroupPanel extends NoteGroupPanel {
    private static final Logger log = LoggerFactory.getLogger(PlainNoteGroupPanel.class);
    private static final int DEFAULT_PAGE_SIZE = 25;
    GroupProperties groupProperties;

    public PlainNoteGroupPanel(GroupInfo groupInfo, int pageSize) {
        super(pageSize);
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty Group.
        myNoteGroup.myNoteGroupPanel = this;
        if (groupInfo.archiveName != null) setEditable(false); // Archived groups are non-editable
        loadNotesPanel();

        // Get the group properties.
        groupProperties = myNoteGroup.getGroupProperties();

        theNotePager.reset(1); // Without this, the pager appears and shows 'page 0 of 0'.
        // But with it, if there are fewer than 2 pages, it remains non-visible.

        buildPanelContent(); // Content other than the groupDataVector
    } // end of the primary constructor

    public PlainNoteGroupPanel(GroupInfo groupInfo) {
        this(groupInfo, DEFAULT_PAGE_SIZE);
    }

    // Called from the constructor to create and place the visual components of the panel.
    private void buildPanelContent() {
        // The multi-row Header for the Log Panel -
        //-----------------------------------------------------
        JPanel heading = new JPanel(new BorderLayout());

        // The First Header Row -   Title
        heading.setBackground(Color.blue);
        String notesName = groupProperties.getGroupName();

        JLabel titleLabel = new JLabel(notesName);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setForeground(Color.white);
        titleLabel.setFont(Font.decode("Serif-bold-20"));
        heading.add(titleLabel, "Center");

        // Set the pager's background to the same color as this row,
        //   since other items on this row make the row slightly taller
        //   than the pager control (pager goes to the top, background shows thru at the bottom).
        theNotePager.setBackground(heading.getBackground());
        heading.add(theNotePager, "East");

        add(heading, BorderLayout.NORTH);
    }


}
