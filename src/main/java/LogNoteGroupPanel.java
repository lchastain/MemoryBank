import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class LogNoteGroupPanel extends NoteGroupPanel {
    private static final Logger log = LoggerFactory.getLogger(LogNoteGroupPanel.class);
    private static final int DEFAULT_PAGE_SIZE = 25;
    LogGroupProperties groupProperties;

    public LogNoteGroupPanel(GroupInfo groupInfo, int pageSize) {
        super(pageSize);
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty NoteGroup.
        myNoteGroup.myNoteGroupPanel = this;
        if (groupInfo.archiveName != null) setEditable(false); // Archived groups are non-editable
        //setAppendable(false); // New notes for this panel will come in at the 'top'.
        loadNotesPanel();

        // Get the group properties.
        groupProperties = (LogGroupProperties) myNoteGroup.getGroupProperties();

        theNotePager.reset(1); // Without this, the pager appears and shows 'page 0 of 0'.
        // But with it, if there are fewer than 2 pages, it remains non-visible.
//        theNotePager.reset(theNotePager.getHighestPage());

        buildPanelContent(); // Content other than the groupDataVector
    } // end of the primary constructor

    public LogNoteGroupPanel(GroupInfo groupInfo) {
        this(groupInfo, DEFAULT_PAGE_SIZE);
    }

    // Called from the constructor to create and place the visual components of the panel.
    // Note to forgetful-me:  When a LogPanel appears as one of a Goal's tabbed panes, the
    //   header here is not actually used.  The goal will manage the pager control, but the
    //   work for the header is needed only for a standalone instance, NOT for a Goal.
    //   This is because the pager and the center panel here are extracted and used by the Goal
    //   when handling a tab change, but do not get added to a container there in the 'normal' way.
    private void buildPanelContent() {
        log.info("Building components for a LogNoteGroupPanel");

        // The multi-row Header for the Log Panel -
        //-----------------------------------------------------
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        // The Header Row -   Title
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

        // The NoteGroup Header Row -   New Log Entry button
        // This is a 'trick' - it is not actually being added to the header, but
        //   prepended as a header to the NoteGroup, via the setGroupHeader call.
        //   But now (March 2025) there is a new approach - not going to use it.
        JPanel headingRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton newEntryButton = new JButton("New Entry");
        newEntryButton.setFont(Font.decode("Serif-bold-14"));
        newEntryButton.setFocusable(false);
        newEntryButton.setPreferredSize(new Dimension(100, 20));
        newEntryButton.addActionListener(e -> { // Make a new, empty log entry.
            LogNoteData logNoteData = new LogNoteData();
            logNoteData.setLogDate(LocalDate.now());
            prependNote(logNoteData);
        });
        headingRow2.add(newEntryButton);

        heading.add(headingRow1);
        add(heading, BorderLayout.NORTH);
//        setGroupHeader(headingRow2);
    } // end buildPanelContent


    @Override
    JComponent makeNewNoteComponent(int i) {
        LogNoteComponent lnc = new LogNoteComponent(this, i);
        lnc.setVisible(false);
        return lnc;
    } // end makeNewNoteComponent


}
