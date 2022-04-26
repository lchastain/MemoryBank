import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

// This class is a grouping of three other NoteGroup panels - Day, Month, and Year.
// Unlike the GoalGroupPanel that is also a grouping, it has no associated data of its own.
// This eliminates the need for special 'header' handling and construction.
public class TabbedCalendarNoteGroupPanel extends NoteGroupPanel implements ChangeListener {
    private static final Logger log = LoggerFactory.getLogger(TabbedCalendarNoteGroupPanel.class);
    JTabbedPane theTabbedPane;

    DayNoteGroupPanel theDayNoteGroupPanel;
    MonthNoteGroupPanel theMonthNoteGroupPanel;
    YearNoteGroupPanel theYearNoteGroupPanel;

    public TabbedCalendarNoteGroupPanel() {
        log.debug("Constructing a TabbedCalendarNoteGroupPanel");
        // We don't call super() here (because we have no data of our own to load or show),
        //   but we still need the NoteGroupPanel.basePanel -
        theBasePanel = new JPanel(new BorderLayout());

        // Before this class is constructed, the AppTreePanel is responsible for (re-)constructing the
        //   three calendar-based NoteGroup panels.
        theDayNoteGroupPanel = AppTreePanel.theInstance.theAppDays;
        theDayNoteGroupPanel.fosterNoteGroupPanel = this; // For menu adjustments.
        theMonthNoteGroupPanel = AppTreePanel.theInstance.theAppMonths;
        theMonthNoteGroupPanel.fosterNoteGroupPanel = this; // For menu adjustments.
        theYearNoteGroupPanel = AppTreePanel.theInstance.theAppYears;
        theYearNoteGroupPanel.fosterNoteGroupPanel = this; // For menu adjustments.

        theTabbedPane = new JTabbedPane();
        theTabbedPane.addTab("Day", theDayNoteGroupPanel.theBasePanel);
        theTabbedPane.addTab("Month", theMonthNoteGroupPanel.theBasePanel);
        theTabbedPane.addTab("Year", theYearNoteGroupPanel.theBasePanel);
        theTabbedPane.addChangeListener(this);

        theBasePanel.add(theTabbedPane, BorderLayout.CENTER);
    } // end constructor

    @Override
    public void stateChanged(ChangeEvent e) {
        //JTabbedPane pane = (JTabbedPane) e.getSource();
        int index = theTabbedPane.getSelectedIndex();
        switch (index) {
            case 0:  // Day Notes
                theDayNoteGroupPanel.adjustMenuItems(theDayNoteGroupPanel.myNoteGroup.groupChanged);
                break;
            case 1: // Month Notes
                theMonthNoteGroupPanel.adjustMenuItems(theMonthNoteGroupPanel.myNoteGroup.groupChanged);
                break;
            case 2: // Year Notes
                theYearNoteGroupPanel.adjustMenuItems(theYearNoteGroupPanel.myNoteGroup.groupChanged);
                break;
        }
    }
} // end class

