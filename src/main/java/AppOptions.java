import java.io.Serializable;
import java.util.Vector;

//-------------------------------------------------------------------------
// Class Name:  AppOptions
//
// The purpose of this class is to preserve the current state of the
//   application - JTree expanded nodes, variable leaf names, current
//   selection, etc.
//-------------------------------------------------------------------------
class AppOptions implements Serializable {
    static final long serialVersionUID = 1654764549994200454L;

    boolean ViewsExpanded;
    boolean NotesExpanded;
    boolean TodoListsExpanded;
    String theSelection;
    int theSelectionRow;
    Vector<String> todoLists;
    SearchResultNode searchResults;
    String thePlaf;     // The chosen Look and Feel (full class name)
    int paneSeparator;  // Position of the separator bar between Left and Right panes.

    public AppOptions() {
        ViewsExpanded = false;
        NotesExpanded = false;
        TodoListsExpanded = false;
        theSelection = null;
        theSelectionRow = -1;
        todoLists = new Vector<String>(0, 1);
        searchResults = null;
    } // end constructor

    public AppOptions(Object theObject) {
        ViewsExpanded = ((AppOptions) theObject).ViewsExpanded;
        NotesExpanded = ((AppOptions) theObject).NotesExpanded;
        TodoListsExpanded = ((AppOptions) theObject).TodoListsExpanded;
        theSelection = ((AppOptions) theObject).theSelection;
        theSelectionRow = ((AppOptions) theObject).theSelectionRow;
        todoLists = ((AppOptions) theObject).todoLists;
        searchResults = ((AppOptions) theObject).searchResults;
    } // end constructor
} // end class AppOptions
