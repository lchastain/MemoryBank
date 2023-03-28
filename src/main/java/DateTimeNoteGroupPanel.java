//import com.fasterxml.jackson.core.type.TypeReference;
//import java.util.Vector;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class DateTimeNoteGroupPanel extends NoteGroupPanel {
    private static final Logger log = LoggerFactory.getLogger(DateTimeNoteGroupPanel.class);
    private static final int DEFAULT_PAGE_SIZE = 20;

    public DateTimeNoteGroupPanel(GroupInfo groupInfo) {
        this(groupInfo, DEFAULT_PAGE_SIZE);
    }

    public DateTimeNoteGroupPanel(String groupName) {
        this(new GroupInfo(groupName, GroupType.NOTES), DEFAULT_PAGE_SIZE);
    }

    public DateTimeNoteGroupPanel(@NotNull GroupInfo groupInfo, int pageSize) {
        super(pageSize);

        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.
        myNoteGroup.myNoteGroupPanel = this;
        if(groupInfo.archiveName != null) setEditable(false); // Archived groups are non-editable
        loadNotesPanel();

        buildMyPanel(groupInfo.getGroupName());
        theNotePager.reset(1);
        setListMenu(AppMenuBar.getNodeMenu("Notes"));
    } // end constructor

    private void buildMyPanel(String groupName) {
        log.info("Building components for a DateTimeGroupPanel named: " + groupName);

        // Create the window title
        JLabel lblListTitle = new JLabel();
        lblListTitle.setHorizontalAlignment(JLabel.CENTER);
        lblListTitle.setForeground(Color.white);
        lblListTitle.setFont(Font.decode("Serif-bold-20"));
        lblListTitle.setText(groupName);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setBackground(Color.blue);
        heading.add(lblListTitle, "Center");

        // Set the pager's background to the same color as this row,
        //   since the title on this row is taller and that makes
        //   a thin slice of 'open space' below the pager.  This is
        //   better than stretching the pager control because that
        //   separator spacing is actually visually preferred.
        theNotePager.setBackground(heading.getBackground());
        heading.add(theNotePager, "East");

        theBasePanel.add(heading, BorderLayout.NORTH);
    } // end buildMyPanel

    //-------------------------------------------------------------------
    // Method Name: makeNewNoteComponent
    //
    // Called (indirectly) by the NoteGroup (base class) constructor,
    //   to populate the groupNotesListPanel.
    //-------------------------------------------------------------------
    @Override
    JComponent makeNewNoteComponent(int i) {
        DateTimeNoteComponent dtnc = new DateTimeNoteComponent(this, i);
        dtnc.setVisible(false);
        return dtnc;
    } // end makeNewNoteComponent

}
