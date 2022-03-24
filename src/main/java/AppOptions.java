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
    String theSelection;
    int theSelectionRow;
    Vector<String> goalsList;
    Vector<String> eventsList;
    Vector<String> tasksList;
    Vector<String> searchResultList;
    int paneSeparator;  // Position of the separator bar between Left and Right panes.

    AppOptions() {
        goalsExpanded = false;
        eventsExpanded = false;
        viewsExpanded = false;
        notesExpanded = false;
        todoListsExpanded = false;
        searchesExpanded = false;
        theSelection = null;
        theSelectionRow = -1;
        goalsList = new Vector<>(0, 1);
        eventsList = new Vector<>(0, 1);
        tasksList = new Vector<>(0, 1);
        searchResultList = new Vector<>(0, 1);
    } // end constructor

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

    static void loadOpts() {
        // TODO - change usages of this method (mostly by tests) to direct assignment of static var, then remove this method.
        MemoryBank.appOpts = MemoryBank.dataAccessor.getAppOptions();
    } // end loadOpts

    static void saveOpts() {
        // TODO - change usages of this method (mostly by tests) to direct invocation of the static method, then remove this method.
        MemoryBank.dataAccessor.saveAppOptions();
    } // end saveOpts
} // end class AppOptions
