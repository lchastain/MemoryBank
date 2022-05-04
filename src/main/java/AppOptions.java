import java.util.Vector;

//-------------------------------------------------------------------------
// Class Name:  AppOptions
//
// The purpose of this class is to preserve the current state of the
//   application - JTree expanded nodes, variable leaf names, current
//   view, etc.  It will be used to restore the app to its previous run state and also
//   for preserving app state for archives.
//
// Considered for storage but not implemented:
// 1.  A custom icon for the app
//     Not that useful for the trouble; a true user will be using a browser.
//     Also, do not want to proliferate different appearances of this somewhat
//     critical component; it could confuse support people, when we get to the
//     point of having support people.
//-------------------------------------------------------------------------
@SuppressWarnings("rawtypes")
class AppOptions {
    boolean goalsExpanded;
    boolean eventsExpanded;
    boolean viewsExpanded;
    boolean notesExpanded;
    boolean todoListsExpanded;
    boolean searchesExpanded;
    boolean groupCalendarNotes;
    String theSelection;
    int theSelectionRow;
    IconInfo defaultDayNoteIconInfo;
    String defaultDayNoteIconDescription;
    IconInfo defaultMonthNoteIconInfo;
    String defaultMonthNoteIconDescription;
    IconInfo defaultYearNoteIconInfo;
    String defaultYearNoteIconDescription;
    IconInfo defaultEventNoteIconInfo;
    String defaultEventNoteIconDescription;
    TimeFormat timeFormat;
    Vector<String> goalsList;
    Vector<String> eventsList;
    Vector<String> tasksList;
    Vector<String> searchResultList;
    int paneSeparator;  // Position of the separator bar between Left and Right panes.

    enum TimeFormat {
        CIVILIAN,  // 12-hour clock
        MILITARY   // 24-hour-clock
    }
    

    AppOptions() {
        goalsExpanded = false;
        eventsExpanded = false;
        viewsExpanded = false;
        notesExpanded = false;
        todoListsExpanded = false;
        searchesExpanded = false;
        groupCalendarNotes = false;
        theSelection = null;
        theSelectionRow = -1;
        goalsList = new Vector<>(0, 1);
        eventsList = new Vector<>(0, 1);
        tasksList = new Vector<>(0, 1);
        searchResultList = new Vector<>(0, 1);
        defaultDayNoteIconInfo = new IconInfo(DataArea.APP_ICONS, "icon_not", "gif");
        defaultDayNoteIconDescription = null;
        defaultMonthNoteIconInfo = new IconInfo(DataArea.APP_ICONS, "calendr2", "gif");
        defaultMonthNoteIconDescription = null;
        defaultYearNoteIconInfo = new IconInfo(DataArea.APP_ICONS, "note2", "gif");
        defaultYearNoteIconDescription = null;
        defaultEventNoteIconInfo = new IconInfo(DataArea.APP_ICONS, "reminder", "gif");
        defaultEventNoteIconDescription = null;
        timeFormat = TimeFormat.CIVILIAN;
    } // end constructor

    // Returns a boolean to indicate whether or not the indicated NoteGroup is present in the
    //   list of NoteGroups that are currently showing on the Tree.
    // Used by Search and Archive operations.
    boolean active(GroupType groupType, String groupName) {
        Vector theList = null;
        switch (groupType) {
            case DAY_NOTES:
            case MONTH_NOTES:
            case YEAR_NOTES:
                // These types are always active.
                return true;
            case GOALS:
                theList = goalsList;
                break;
            case EVENTS:
                theList = eventsList;
                break;
            case TODO_LIST:
                theList = tasksList;
                break;
            case SEARCH_RESULTS:
                theList = searchResultList;
                break;
        }
        if(theList == null) return false;

        for (Object o : theList) {
            String object = (String) o;
            if(groupName.equals(object)) return true;
        }
        return false;
    }

    //=================================================================================================================
    // Below are two static methods that simply make a call to a static method in the app main -
    // They have evolved from code that originally did all the work here, to now de-referenced one-liners.
    // But the code that uses them (admittedly mostly tests) is more readable and more easily maintained by leaving
    //  these methods in place and with their simpler signatures, as opposed to refactoring the calling contexts to
    //  make the static calls directly.
    //=================================================================================================================
    static void loadOpts() {
        MemoryBank.appOpts = MemoryBank.dataAccessor.getAppOptions();
    } // end loadOpts

    static void saveOpts() {
        MemoryBank.dataAccessor.saveAppOptions();
    } // end saveOpts

    // Used by the Jackson mapper, upon data load and conversion of json to an object of this class.
    void setDefaultDayNoteIconDescription(String s) {
        if(s == null || s.isEmpty()) return;
        defaultDayNoteIconDescription = s;
        defaultDayNoteIconInfo = null;
    }

    // Used by the Jackson mapper, upon data load and conversion of json to an object of this class.
    void setDefaultEventNoteIconDescription(String s) {
        if(s == null || s.isEmpty()) return;
        defaultEventNoteIconDescription = s;
        defaultEventNoteIconInfo = null;
    }
} // end class AppOptions
