import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// This class is a grouping of three other NoteGroup panels - Day, Month, and Year.
// It is constructed much more straightforwardly and less surgically than is the GoalGroupPanel.
public class TabbedCalendarNoteGroupPanel implements AlteredDateListener {
    private static final Logger log = LoggerFactory.getLogger(TabbedCalendarNoteGroupPanel.class);
    JPanel theBasePanel;
    JTabbedPane theTabbedPane;

    DayNoteGroupPanel theDayNoteGroupPanel;
    MonthNoteGroupPanel theMonthNoteGroupPanel;
    YearNoteGroupPanel theYearNoteGroupPanel;

    public TabbedCalendarNoteGroupPanel() {
        theDayNoteGroupPanel = new DayNoteGroupPanel();
        theMonthNoteGroupPanel = new MonthNoteGroupPanel();
        theYearNoteGroupPanel = new YearNoteGroupPanel();

        theDayNoteGroupPanel.setAlteredDateListener(this);
        theMonthNoteGroupPanel.setAlteredDateListener(this);
        theYearNoteGroupPanel.setAlteredDateListener(this);

        theBasePanel = new JPanel(new BorderLayout());
        theTabbedPane = new JTabbedPane();
        theTabbedPane.addTab("Day", theDayNoteGroupPanel.theBasePanel);
        theTabbedPane.addTab("Month", theMonthNoteGroupPanel.theBasePanel);
        theTabbedPane.addTab("Year", theYearNoteGroupPanel.theBasePanel);

        theBasePanel.add(theTabbedPane, BorderLayout.CENTER);
    }

    void dateChanged (LocalDate theNewDate) {
        int theIndex = theTabbedPane.getSelectedIndex();
        switch (theIndex) {
            case 0:
                theMonthNoteGroupPanel.setDate(theNewDate);
                theYearNoteGroupPanel.setDate(theNewDate);
                break;
            case 1:
                theDayNoteGroupPanel.setDate(theNewDate);
                theYearNoteGroupPanel.setDate(theNewDate);
                break;
            case 2:
                theDayNoteGroupPanel.setDate(theNewDate);
                theMonthNoteGroupPanel.setDate(theNewDate);
        }
    }

    @Override // AlteredDateListener method
    public void dateDecremented(LocalDate theNewDate, ChronoUnit theGranularity) {
        dateChanged(theNewDate);
    }

    @Override // AlteredDateListener method
    public void dateIncremented(LocalDate theNewDate, ChronoUnit theGranularity) {
        dateChanged(theNewDate);
    }


}
