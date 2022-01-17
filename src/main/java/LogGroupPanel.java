import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;

public class LogGroupPanel extends NoteGroupPanel {
    private static final Logger log = LoggerFactory.getLogger(LogGroupPanel.class);
    LogGroupProperties groupProperties;       // Needs to be at this level?
    GoalGroupProperties goalGroupProperties;  // Needs to be at this level?
    JLabel titleLabel;


    public LogGroupPanel(GroupInfo groupInfo) {
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty GoalGroup.
        myNoteGroup.myNoteGroupPanel = this;
        if (groupInfo.archiveName != null) setEditable(false); // Archived groups are non-editable
        setAppendable(false); // New notes for this panel will come in at the 'top'.
        loadNotesPanel();

        // get the right type of parent group properties, for use in the Log panel header.
        groupProperties = (LogGroupProperties) myNoteGroup.getGroupProperties();
        goalGroupProperties = (GoalGroupProperties) groupProperties.parentGroupProperties; // could be null

        buildPanelContent(); // Content other than the groupDataVector
    }


    public LogGroupPanel(String groupName) {
        this(new GroupInfo(groupName, GroupType.LOG));
    }


    // Called from within the constructor to create and place the visual components of the panel.
    @SuppressWarnings({"rawtypes"})
    private void buildPanelContent() {
        // The multi-row Header for the GoalGroup -
        //-----------------------------------------------------
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        // The First Header Row -   Title
        JPanel headingRow1 = new JPanel(new BorderLayout()); // Need to put the title into a separate panel, because -
        headingRow1.setBackground(Color.blue); // it covers width of the panel, not just the length of the title.
        String logName = myNoteGroup.myProperties.getGroupName();

        // HERE is where we make some branches depending on the makeup of the parentGroupProperties.
        if (goalGroupProperties != null) {
            String goalPlan = goalGroupProperties.goalPlan;
            String longTitle = goalGroupProperties.longTitle;
            if (longTitle == null || longTitle.isEmpty()) longTitle = logName;
            titleLabel = new JLabel(longTitle);
            titleLabel.setText(longTitle);
            titleLabel.setHorizontalAlignment(JLabel.CENTER);
            titleLabel.setForeground(Color.white);
            titleLabel.setFont(Font.decode("Serif-bold-20"));
            if (goalPlan != null && !goalPlan.trim().isEmpty()) {
                String theTip = AppUtil.getTooltipString(goalPlan);
                // Wrap in HTML and PREserve the original formatting, to hold on to indents and multi-line.
                theTip = "<html><pre>" + theTip + "</pre></html>";
                titleLabel.setToolTipText(theTip);
            } else {
                titleLabel.setToolTipText("Click here to enter / edit the Goal plan");
            }

            // Use a NoteData to hold the longer title and the Plan.
            NoteData titleNoteData = new NoteData(logName); // In this case the noteString does not get used.
            titleNoteData.setSubjectString(longTitle);
            titleNoteData.setExtendedNoteString(goalPlan);

            titleLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    boolean planChanged = editExtendedNoteComponent(titleNoteData);
                    if (planChanged) {
                        if (titleNoteData.subjectString.isEmpty()) {
                            titleNoteData.subjectString = logName;
                            // In case the user cleared the entry completely; we don't want the title line to go away.
                            // But a single space - makes a seemingly empty blue title line.  If that's what they want
                            // to see then we allow it; at least it is still re-selectable, to change to something else.
                        }
                        titleLabel.setText(titleNoteData.subjectString);
                        // set the values in group properties...
                        String goalPlan = titleNoteData.extendedNoteString;
                        ((GoalGroupProperties) myNoteGroup.myProperties).longTitle = titleNoteData.subjectString;
                        ((GoalGroupProperties) myNoteGroup.myProperties).goalPlan = goalPlan;
                        if (goalPlan != null && !goalPlan.trim().isEmpty()) {
                            titleLabel.setToolTipText(goalPlan);
                        } else {
                            titleLabel.setToolTipText("Click here to enter / edit the Goal plan");
                        }

                        setGroupChanged(true);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    setStatusMessage("Click here to edit your Goal Name and Plan");
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setStatusMessage(" ");
                }
            });
            headingRow1.add(titleLabel, "Center");
        }

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
        heading.add(headingRow2);
        add(heading, BorderLayout.NORTH);
    }


//    @Override
//    public boolean editExtendedNoteComponent(NoteData noteData) {
//        if(noteData instanceof TodoNoteData) {
//            // Let the base class make this -
//            extendedNoteComponent = null;
//            setDefaultSubject(null);
//        } else {
//            setDefaultSubject("Goal Title"); // Panel uses this when calling editExtendedNoteComponent.
//            // Prevent base class from constructing its own.
//            extendedNoteComponent = new ExtendedNoteComponent("Goal Title");
//            extendedNoteComponent.setPhantomText(userInfo);
//        }
//        return super.editExtendedNoteComponent(noteData);
//    }


    @Override
    JComponent makeNewNote(int i) {
        LogNoteComponent lnc = new LogNoteComponent(this, i);
        lnc.setVisible(false);
        return lnc;
    } // end makeNewNote

//    @Override
//    public void shiftDown(int index) {
//        if (index >= lastVisibleNoteIndex) return;
//        System.out.println("Log Group Panel Shifting note down");
//        LogNoteComponent src1, src2;
//        src1 = (LogNoteComponent) groupNotesListPanel.getComponent(index);
//        src2 = (LogNoteComponent) groupNotesListPanel.getComponent(index + 1);
//
//        src1.swap(src2);
//        src2.setActive();
//    } // end shiftDown
//
//
//    @Override
//    public void shiftUp(int index) {
//        if (index == 0) return;
//        System.out.println("Log Group Panel Shifting note up");
//        LogNoteComponent src1, src2;
//        src1 = (LogNoteComponent) groupNotesListPanel.getComponent(index);
//        src2 = (LogNoteComponent) groupNotesListPanel.getComponent(index - 1);
//
//        src1.swap(src2);
//        src2.setActive();
//    } // end shiftUp


}
