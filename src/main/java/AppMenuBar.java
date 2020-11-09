import javax.swing.*;

public class AppMenuBar extends JMenuBar{
    static final long serialVersionUID = 1L; // JMenuBar wants this but we will not serialize.

    // Menus
    //-------------------------------------------------
    private static final JMenu fileMenu;
    private static final JMenu branchEditorMenu;
    private static final JMenu deletedMenu;

    private static final JMenu goalsMenu;
    private static final JMenu eventsMenu;
    private static final JMenu viewsMenu;
    private static final JMenu notesMenu;
    private static final JMenu todolistsMenu;
    private static final JMenu searchesMenu;

    private static final JMenu helpMenu;
    //-------------------------------------------------

    private String theCurrentContext;
    private boolean showDeleteUndo;

    static {
        fileMenu = new JMenu("App");
        fileMenu.add(new JMenuItem("Search..."));
        fileMenu.add(new JMenuItem("Show Scheduled Events"));
        fileMenu.add(new JMenuItem("Show Current NoteGroup"));
        fileMenu.add(new JMenuItem("Icon Manager..."));
        fileMenu.add(new JMenuItem("Exit"));

        branchEditorMenu = new JMenu("List");
        branchEditorMenu.add(new JMenuItem("Add New..."));

        deletedMenu = new JMenu("Deleted");
        deletedMenu.add(new JMenuItem("Undo Delete"));

        goalsMenu = new JMenu("Goal");
        goalsMenu.add(new JMenuItem("Undo All"));
        goalsMenu.add(new JMenuItem("Close"));
        goalsMenu.add(new JMenuItem("Group Linkages..."));
        goalsMenu.add(new JMenuItem("Add New..."));
        goalsMenu.add(new JMenuItem("Save"));
        goalsMenu.add(new JMenuItem("Save As..."));
        goalsMenu.add(new JMenuItem("Clear All"));
        goalsMenu.add(new JMenuItem("Delete"));

        eventsMenu = new JMenu("Upcoming Event");
        eventsMenu.add(new JMenuItem("Undo All"));
        eventsMenu.add(new JMenuItem("Close"));
        eventsMenu.add(new JMenuItem("Group Linkages..."));
        eventsMenu.add(new JMenuItem("Add New..."));
        eventsMenu.add(new JMenuItem("Merge..."));
        eventsMenu.add(new JMenuItem("Save"));
        eventsMenu.add(new JMenuItem("Save As..."));
        eventsMenu.add(new JMenuItem("Clear All"));
        eventsMenu.add(new JMenuItem("Delete"));

        viewsMenu = new JMenu("View");
        viewsMenu.add(new JMenuItem("Today"));

        notesMenu = new JMenu("Notes");
        notesMenu.add(new JMenuItem("Undo All"));
        notesMenu.add(new JMenuItem("Group Linkages..."));
        notesMenu.add(new JMenuItem("Today"));
        notesMenu.add(new JMenuItem("Save"));
        notesMenu.add(new JMenuItem("Clear All"));

        todolistsMenu = new JMenu("To Do List");
        todolistsMenu.add(new JMenuItem("Undo All"));
        todolistsMenu.add(new JMenuItem("Close"));
        todolistsMenu.add(new JMenuItem("Group Linkages..."));
        todolistsMenu.add(new JMenuItem("Add New..."));
        todolistsMenu.add(new JMenuItem("Merge..."));
        todolistsMenu.add(new JMenuItem("Print..."));
        todolistsMenu.add(new JMenuItem("Save"));
        todolistsMenu.add(new JMenuItem("Save As..."));
        todolistsMenu.add(new JMenuItem("Clear All"));
        todolistsMenu.add(new JMenuItem("Delete"));

        searchesMenu = new JMenu("Search Results");
        searchesMenu.add(new JMenuItem("Close"));
        searchesMenu.add(new JMenuItem("Review...")); // Not yet working..
        searchesMenu.add(new JMenuItem("Delete"));

        helpMenu = new JMenu("Help");
        helpMenu.add(new JMenuItem("Contents"));
        helpMenu.add(new JMenuItem("About"));
    } // end static

    public AppMenuBar() {
        super();
        add(fileMenu);
        add(branchEditorMenu);
        add(deletedMenu);
        add(goalsMenu);
        add(eventsMenu);
        add(viewsMenu);
        add(notesMenu);
        add(todolistsMenu);
        add(searchesMenu);
        showDeleteUndo = false;

        // This puts the 'Help' on the far right side.
        add(Box.createHorizontalGlue());
        add(helpMenu);
        // mb.setHelpMenu(menuHelp);  // Not implemented in Java 1.4.2 ...
        // Still not implemented in Java 1.5.0_03
        // In Java 1.8, throws a Not Implemented exception
    }

    String getCurrentContext() {
        return theCurrentContext;
    }

    // Get the additional menu that is appropriate for the selected tree node.
    // This is NOT the main handler; just answers the 'what menu was this?' question.
    JMenu getNodeMenu(String selectionContext) {
        JMenu theMenu;
        switch (selectionContext) {
            case "Year View":
            case "Month View":
            case "Week View":
                theMenu = viewsMenu;
                break;
            case "Day Notes":
            case "Month Notes":
            case "Year Notes":
                theMenu = notesMenu;
                break;
            case "Search Result":
                theMenu = searchesMenu;
                searchesMenu.setVisible(true);
                break;
            case "Goals Branch Editor":
            case "Upcoming Events Branch Editor":  // Upcoming Events
            case "To Do Lists Branch Editor":  // TodoBranchHelper
                theMenu = branchEditorMenu;
                break;
            case "Goal":
                theMenu = goalsMenu;
                break;
            case "Upcoming Event":
                theMenu = eventsMenu;
                break;
            case "To Do List":
                theMenu = todolistsMenu;
                break;
            default: // No additional menu is defined for the specified node.
                theMenu = null;
                break;
        }
        return theMenu;
    } // end getNodeMenu

    // Given a string to indicate what Tree node is selected,
    // display the menu that is appropriate to that node.
    void manageMenus(String theContext) {
        theCurrentContext = theContext;

        // The default is to have the 'File' and 'Help' menus only;
        //   let the specific cases below make any needed addition.
        //-----------------------------------------

        branchEditorMenu.setVisible(false);
        goalsMenu.setVisible(false);
        eventsMenu.setVisible(false);
        viewsMenu.setVisible(false);
        notesMenu.setVisible(false);
        todolistsMenu.setVisible(false);
        searchesMenu.setVisible(false);

        deletedMenu.setVisible(showDeleteUndo);

        MemoryBank.debug("Setting MenuBar Configuration: " + theCurrentContext);

        switch (theCurrentContext) {
            case "Year View":  // Year View
            case "Month View":  // Month View
            case "Week View":
                viewsMenu.setVisible(true);
                break;
            case "Day Notes":  // Day Notes
            case "Month Notes":  // Month Notes
            case "Year Notes":  // Year Notes
                notesMenu.setVisible(true);
                break;
            case "Search Result":  // Search Results
                searchesMenu.setVisible(true);
                break;
            case "Goals Branch Editor":
                branchEditorMenu.setText("Goals");
                branchEditorMenu.setVisible(true);
                break;
            case "Upcoming Events Branch Editor":  // Upcoming Events
                branchEditorMenu.setText("Events");
                branchEditorMenu.setVisible(true);
                break;
            case "To Do Lists Branch Editor":  // TodoBranchHelper
                branchEditorMenu.setText("To Do Lists");
                branchEditorMenu.setVisible(true);
                break;
            case "Goal":
                goalsMenu.setVisible(true);
                break;
            case "Upcoming Event":
                eventsMenu.setVisible(true);
                break;
            case "To Do List":       // A List
                todolistsMenu.setVisible(true);
                break;
            default: // No special handling but still a valid configuration.
                // Expected examples:  About, Today (conditional), Branches without an editor.
                // Unexpected - will only show up during development.
                break;
        }
    } // end manageMenus

    void showRestoreOption(boolean showIt) {
        showDeleteUndo = showIt;
    }

} // end class AppMenuBar