import java.util.Vector;

//-------------------------------------------------------------------------
// Class Name:  AppOptions
//
// The purpose of this class is to preserve the current state of the
//   application - JTree expanded nodes, variable leaf names, current
//   selection, etc.
//
// Considered for storage but not implemented:
// 1.  A custom icon for the app
//     Not that useful for the trouble; a true user will be using a browser.
//     Also, do not want to proliferate different appearances of this somewhat
//     critical component; it could confuse support people, when we get to the
//     point of having support people.
//-------------------------------------------------------------------------
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
    String consolidatedEventsViewName;

    AppOptions() {
        goalsExpanded = false;
        eventsExpanded = false;
        viewsExpanded = false;
        notesExpanded = false;
        todoListsExpanded = false;
        searchesExpanded = false;
        theSelection = null;
        theSelectionRow = -1;
        consolidatedEventsViewName = "Consolidated View";
        goalsList = new Vector<>(0, 1);
        eventsList = new Vector<>(0, 1);
        tasksList = new Vector<>(0, 1);
        searchResultList = new Vector<>(0, 1);
    } // end constructor

} // end class AppOptions
