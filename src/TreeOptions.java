import java.io.Serializable;
import java.util.Vector;

//-------------------------------------------------------------------------
// Class Name:  TreeOptions
//
// The purpose of this class is to preserve the state of the AppTree 'tree' -
//   expanded nodes, variable leaf names, current selection.
//-------------------------------------------------------------------------
class TreeOptions implements Serializable {
    static final long serialVersionUID = -7794718588806876785L;

    boolean ViewsExpanded;
    boolean NotesExpanded;
    boolean TodoListsExpanded;
    String theSelection;
    int theSelectionRow;
    Vector<String> todoLists;
    SearchResultNode searchResults;

    public TreeOptions() {
        ViewsExpanded = false;
        NotesExpanded = false;
        TodoListsExpanded = false;
        theSelection = null;
        theSelectionRow = -1;
        todoLists = new Vector<String>(0, 1);
        searchResults = null;
    } // end constructor
} // end class TreeOptions
