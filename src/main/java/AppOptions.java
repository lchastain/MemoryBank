import java.io.Serializable;
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
//     critical component; it could confuse support people, when we get to that point.
//-------------------------------------------------------------------------
class AppOptions implements Serializable {
    static final long serialVersionUID = 1654764549994200454L;

    boolean ViewsExpanded;
    boolean NotesExpanded;
    boolean TodoListsExpanded;
    String theSelection;
    int theSelectionRow;
    Vector<String> todoLists;
    Vector<String> searchResultList;
    int paneSeparator;  // Position of the separator bar between Left and Right panes.
    boolean military;

    public AppOptions() {
        ViewsExpanded = false;
        NotesExpanded = false;
        TodoListsExpanded = false;
        theSelection = null;
        theSelectionRow = -1;
        todoLists = new Vector<>(0, 1);
        searchResultList = new Vector<>(0, 1);
        military = false;
    } // end constructor

    public AppOptions(Object theObject) {
        ViewsExpanded = ((AppOptions) theObject).ViewsExpanded;
        NotesExpanded = ((AppOptions) theObject).NotesExpanded;
        TodoListsExpanded = ((AppOptions) theObject).TodoListsExpanded;
        theSelection = ((AppOptions) theObject).theSelection;
        theSelectionRow = ((AppOptions) theObject).theSelectionRow;
        todoLists = ((AppOptions) theObject).todoLists;
        searchResultList = ((AppOptions) theObject).searchResultList;
        paneSeparator = ((AppOptions) theObject).paneSeparator;
        military = ((AppOptions) theObject).military;
    } // end constructor

} // end class AppOptions
