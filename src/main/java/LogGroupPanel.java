import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;

public class LogGroupPanel extends NoteGroupPanel {
    private static final Logger log = LoggerFactory.getLogger(LogGroupPanel.class);
    LogGroupProperties groupProperties;


    public LogGroupPanel(GroupInfo groupInfo) {
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty GoalGroup.
        myNoteGroup.myNoteGroupPanel = this;
        if (groupInfo.archiveName != null) setEditable(false); // Archived groups are non-editable
        setAppendable(false); // New notes for this panel will come in at the 'top'.
        loadNotesPanel();

        // Get the group properties and set a 'parent' group, if there is one.
        groupProperties = (LogGroupProperties) myNoteGroup.getGroupProperties();

        buildPanelContent(); // Content other than the groupDataVector
    }


    public LogGroupPanel(String groupName) {
        this(new GroupInfo(groupName, GroupType.LOG));
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
                LogData logNoteData = new LogData();
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
