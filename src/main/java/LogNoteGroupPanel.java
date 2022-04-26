import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;

public class LogNoteGroupPanel extends NoteGroupPanel {
    private static final Logger log = LoggerFactory.getLogger(LogNoteGroupPanel.class);
    private static final int DEFAULT_PAGE_SIZE = 25;
    LogGroupProperties groupProperties;

    public LogNoteGroupPanel(GroupInfo groupInfo, int pageSize) {
        super(pageSize);
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty GoalGroup.
        myNoteGroup.myNoteGroupPanel = this;
        if (groupInfo.archiveName != null) setEditable(false); // Archived groups are non-editable
        setAppendable(false); // New notes for this panel will come in at the 'top'.
        loadNotesPanel();

        // Get the group properties.
        groupProperties = (LogGroupProperties) myNoteGroup.getGroupProperties();

        theNotePager.reset(1); // Without this, the pager appears and shows 'page 0 of 0'.
        // But with it, if there are fewer than 2 pages, it remains non-visible.

        buildPanelContent(); // Content other than the groupDataVector
    } // end of the primary constructor


    public LogNoteGroupPanel(GroupInfo groupInfo) {
        this(groupInfo, DEFAULT_PAGE_SIZE);
    }

    public LogNoteGroupPanel(String groupName) {
        this(new GroupInfo(groupName, GroupType.LOG), DEFAULT_PAGE_SIZE);
    }


    @Override
    protected void adjustMenuItems(boolean b) {
        MemoryBank.debug("LogNoteGroupPanel.adjustMenuItems <" + b + ">");
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
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        // The First Header Row -   Title
        JPanel headingRow1 = new JPanel(new BorderLayout());
        headingRow1.setBackground(Color.blue);
        String logName = groupProperties.getGroupName();

        JLabel titleLabel = new JLabel(logName);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setForeground(Color.white);
        titleLabel.setFont(Font.decode("Serif-bold-20"));
        headingRow1.add(titleLabel, "Center");

        // Set the pager's background to the same color as this row,
        //   since other items on this row make the row slightly taller
        //   than the pager control (pager goes to the top, background shows thru at the bottom).
        theNotePager.setBackground(headingRow1.getBackground());
        headingRow1.add(theNotePager, "East");

        // The Second Header Row -   New Log Entry
        JPanel headingRow2 = new JPanel(new DndLayout());

        JLabel newEntryLabel = new JLabel(" New Entry: ");
        newEntryLabel.setHorizontalAlignment(JLabel.CENTER);
        newEntryLabel.setFont(Font.decode("Serif-bold-14"));
        headingRow2.add(newEntryLabel);

        JTextField newEntryField = new JTextField();
        newEntryField.setFont(Font.decode("Serif-bold-12"));
        newEntryField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField jtf = (JTextField) e.getSource();
                String theEntry = jtf.getText();
                if (theEntry == null || theEntry.isEmpty()) return;
                jtf.setText(null); // Clear the input field, make ready for the next entry.

                // Make a new log entry from the user-entered text.
                LogNoteData logNoteData = new LogNoteData();
                logNoteData.setNoteString(theEntry);
                logNoteData.setLogDate(LocalDate.now());

                prependNote(logNoteData);

                System.out.println(theEntry);
            }
        });
        headingRow2.add(newEntryField, "Stretch");
        headingRow2.add(new JLabel("  "));

        heading.add(headingRow1);
//        heading.add(headingRow2);
        add(heading, BorderLayout.NORTH);
        setGroupHeader(headingRow2);
    }


    @Override
    JComponent makeNewNote(int i) {
        LogNoteComponent lnc = new LogNoteComponent(this, i);
        lnc.setVisible(false);
        return lnc;
    } // end makeNewNote


}
