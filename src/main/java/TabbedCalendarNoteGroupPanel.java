import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

// This class is a grouping of three other NoteGroup panels - Day, Month, and Year.
// It is constructed much more straightforwardly and less surgically than is the GoalGroupPanel.
@SuppressWarnings("unchecked")
public class TabbedCalendarNoteGroupPanel {
    private static final Logger log = LoggerFactory.getLogger(TabbedCalendarNoteGroupPanel.class);
    JPanel theBasePanel;

    DayNoteGroupPanel theDayNoteGroupPanel;
    MonthNoteGroupPanel theMonthNoteGroupPanel;
    YearNoteGroupPanel theYearNoteGroupPanel;

    CalendarTabType currentCalendarTabType;

    public enum CalendarTabType {
        DAY(0),
        MONTH(1),
        YEAR(2);

        private final int value;
        private static final Map map = new HashMap<>();

        CalendarTabType(int value) {
            this.value = value;
        }

        static {
            for (CalendarTabType pageType : CalendarTabType.values()) {
                map.put(pageType.value, pageType);
            }
        }

        public static CalendarTabType valueOf(int pageType) {
            return (CalendarTabType) map.get(pageType);
        }

        public int getValue() {
            return value;
        }
    } // end enum CalendarType

    public TabbedCalendarNoteGroupPanel() {
        currentCalendarTabType = CalendarTabType.DAY;
        theDayNoteGroupPanel = new DayNoteGroupPanel();
        theMonthNoteGroupPanel = new MonthNoteGroupPanel();
        theYearNoteGroupPanel = new YearNoteGroupPanel();

        theBasePanel = new JPanel(new BorderLayout());
        JTabbedPane theTabbedPane = new JTabbedPane();
        theTabbedPane.addTab("Day", theDayNoteGroupPanel.theBasePanel);
        theTabbedPane.addTab("Month", theMonthNoteGroupPanel.theBasePanel);
        theTabbedPane.addTab("Year", theYearNoteGroupPanel.theBasePanel);

        theBasePanel.add(theTabbedPane, BorderLayout.CENTER);

    }

//    public TabbedCalendarNoteGroupPanel(GroupInfo groupInfo) {
//        super();
//        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty GoalGroup.
//        myNoteGroup.myNoteGroupPanel = this;
//        currentCalendarTabType = CalendarTabType.DAY;
//
////        groupProperties = (GoalGroupProperties) myNoteGroup.getGroupProperties();
//
//        buildPanelContent(); // Content other than the groupDataVector
//    } // end constructor


    @SuppressWarnings({"rawtypes"})
        // Returns a JTabbedPane where the first tab holds the Day and the remaining tabs remain null.
        // Visually this works even when tabs are changed; for actual content changes, the
        // JTabbedPane's changeListener handles that, to make it 'look' like the tabs hold the content when
        // in reality the content of the center of the basePanel is just swapped out.
    JComponent buildHeader() {
        // Now the tabbed pane part -
        JTabbedPane theTabbedPane = new JTabbedPane();
        theTabbedPane.addTab("Day", null);
        theTabbedPane.addTab("Month", null);
        theTabbedPane.addTab("Year", null);

        theTabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JTabbedPane pane = (JTabbedPane) e.getSource();
                int index = pane.getSelectedIndex();
                currentCalendarTabType = CalendarTabType.valueOf(index);
                //System.out.println(index);
//                switch (currentCalendarTabType) {
//                    case DAY:  // To Do List
//                        theBasePanel.remove(theMonthCenterPanel);
//                        theBasePanel.remove(theYearCenterPanel);
//                        theBasePanel.add(theDayCenterPanel, BorderLayout.CENTER);
//                        break;
//                    case MONTH: // Log Entries
//                        theBasePanel.remove(theDayCenterPanel);
//                        theBasePanel.remove(theYearCenterPanel);
//                        theBasePanel.add(theMonthCenterPanel, BorderLayout.CENTER);
//                        break;
//                    case YEAR: // Milestones
//                        theBasePanel.remove(theDayCenterPanel);
//                        theBasePanel.remove(theMonthCenterPanel);
//                        theBasePanel.add(theYearCenterPanel, BorderLayout.CENTER);
//                }
//                theBasePanel.validate();
//                theBasePanel.repaint();
            }
        });

        return theTabbedPane;
    } // end buildHeader


    // Called from within the constructor to create and place the visual components of the panel.
    // The view will be a Tabbed Pane, with the initial Tab showing a ToDo List.
//    @SuppressWarnings({"rawtypes"})
//    private void buildPanelContent() {
//        GroupInfo theGroupInfo;  // Support an archiveName, if there is one.
//        // Make a TodoNoteGroupPanel and get its center component (used when switching tabs)
//        theGroupInfo = new GroupInfo(getGroupName(), GroupType.GOAL_TODO);
//        theGroupInfo.archiveName = myNoteGroup.myGroupInfo.archiveName;
////        theDayNoteGroupPanel = new TodoNoteGroupPanel(theGroupInfo);
//        theDayNoteGroupPanel.parentNoteGroupPanel = this; // For menu adjustments.
//        BorderLayout theTodoLayout = (BorderLayout) theDayNoteGroupPanel.theBasePanel.getLayout();
//        theDayCenterPanel = (JComponent) theTodoLayout.getLayoutComponent(BorderLayout.CENTER);
//
//        // Make a LogGroupPanel and get its center component (used when switching tabs)
//        theGroupInfo = new GroupInfo(getGroupName(), GroupType.GOAL_LOG);
//        theGroupInfo.archiveName = myNoteGroup.myGroupInfo.archiveName;
//        theMonthNoteGroupPanel = new LogNoteGroupPanel(theGroupInfo);
//        theMonthNoteGroupPanel.parentNoteGroupPanel = this; // For menu adjustments.
//        BorderLayout theLogLayout = (BorderLayout) theMonthNoteGroupPanel.theBasePanel.getLayout();
//        theMonthCenterPanel = (JComponent) theLogLayout.getLayoutComponent(BorderLayout.CENTER);
//
//        // Make a MilestoneNoteGroupPanel and get its center component (used when switching tabs)
//        theGroupInfo = new GroupInfo(getGroupName(), GroupType.MILESTONE);
//        theGroupInfo.archiveName = myNoteGroup.myGroupInfo.archiveName;
//        theYearNoteGroupPanel = new MilestoneNoteGroupPanel(theGroupInfo);
//        theYearNoteGroupPanel.parentNoteGroupPanel = this; // For menu adjustments.
//        BorderLayout theLayout = (BorderLayout) theYearNoteGroupPanel.theBasePanel.getLayout();
//        theYearCenterPanel = (JComponent) theLayout.getLayoutComponent(BorderLayout.CENTER);
//
//        JComponent theHeader = buildHeader();
//        add(theHeader, BorderLayout.NORTH);            // Adds to theBasePanel
//        add(theDayCenterPanel, BorderLayout.CENTER);
//    }


}
