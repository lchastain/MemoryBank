import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class PlainNoteGroupPanel extends NoteGroupPanel {
    private static final Logger log = LoggerFactory.getLogger(PlainNoteGroupPanel.class);
    private static final int DEFAULT_PAGE_SIZE = 25;
    GroupProperties groupProperties;

    public PlainNoteGroupPanel(GroupInfo groupInfo, int pageSize) {
        super(pageSize);
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty GoalGroup.
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

    @Override
    protected void adjustMenuItems(boolean b) {
        MemoryBank.debug("PlainNoteGroupPanel.adjustMenuItems <" + b + ">");
        if(fosterNoteGroupPanel != null) { // This NoteGroupPanel is one tab of a collection.
            fosterNoteGroupPanel.adjustMenuItems(b);
        } else {
            super.adjustMenuItems(b);
        }
    }

    // Called from the constructor to create and place the visual components of the panel.
    @SuppressWarnings({"rawtypes"})
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
