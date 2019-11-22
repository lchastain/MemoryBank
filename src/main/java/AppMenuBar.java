import javax.swing.*;

public class AppMenuBar extends JMenuBar{
    static final long serialVersionUID = 1L; // JMenuBar wants this but we will not serialize.

    // Menus
    //-------------------------------------------------
    private static JMenu fileMenu;
    private static JMenu branchEditorMenu;
    private static JMenu eventsMenu;
    private static JMenu viewsMenu;
    private static JMenu notesMenu;
    private static JMenu todolistsMenu;
    private static JMenu searchesMenu;


    private static JMenu branchFileMenu;
    private static JMenu todoFileMenu;
    private static JMenu searchResultFileMenu;

    private static JMenu dayNoteEditMenu;
    private static JMenu monthEditMenu;
    private static JMenu yearEditMenu;
    private static JMenu todoEditMenu;

    private static JMenu viewMenu;
    private static JMenu viewsViewMenu;
    private static JMenu noteViewMenu;

    private static JMenu helpMenu;
    //-------------------------------------------------

    private String theCurrentContext;

    static {
        fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem("Search..."));
        fileMenu.add(new JMenuItem("Icon Manager..."));
        fileMenu.add(new JMenuItem("Exit"));

        branchEditorMenu = new JMenu("Branch Editor");
        branchEditorMenu.add(new JMenuItem("Add New..."));

        eventsMenu = new JMenu("Events");
        eventsMenu.add(new JMenuItem("undo"));
        eventsMenu.add(new JMenuItem("Close"));
        eventsMenu.add(new JMenuItem("Add New..."));
        eventsMenu.add(new JMenuItem("Merge..."));
        eventsMenu.add(new JMenuItem("Save As..."));
        eventsMenu.add(new JMenuItem("Refresh"));
        eventsMenu.add(new JMenuItem("Clear All"));

        viewsMenu = new JMenu("Views");
        viewsMenu.add(new JMenuItem("Today"));

        notesMenu = new JMenu("Notes");
        notesMenu.add(new JMenuItem("undo"));
        notesMenu.add(new JMenuItem("Today"));
        notesMenu.add(new JMenuItem("Refresh"));
        notesMenu.add(new JMenuItem("Clear All"));

        todolistsMenu = new JMenu("ToDo Lists");
        todolistsMenu.add(new JMenuItem("undo"));
        todolistsMenu.add(new JMenuItem("Close"));
        todolistsMenu.add(new JMenuItem("Add New..."));
        todolistsMenu.add(new JMenuItem("Merge..."));
        todolistsMenu.add(new JMenuItem("Print..."));
        todolistsMenu.add(new JMenuItem("Save As..."));
        todolistsMenu.add(new JMenuItem("Refresh"));
        todolistsMenu.add(new JMenuItem("Clear All"));

        searchesMenu = new JMenu("Searches");
        searchesMenu.add(new JMenuItem("Close"));
        searchesMenu.add(new JMenuItem("Review..."));

        helpMenu = new JMenu("Help");
        helpMenu.add(new JMenuItem("Contents"));
        helpMenu.add(new JMenuItem("About"));
    } // end static

    public AppMenuBar() {
        super();
        add(fileMenu);
        add(branchEditorMenu);
        add(eventsMenu);
        add(viewsMenu);
        add(notesMenu);
        add(todolistsMenu);
        add(searchesMenu);

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

    // Given a string to indicate what 'mode' we are in,
    // display the menu that is appropriate to that mode.
    void manageMenus(String theContext) {
        theCurrentContext = theContext;

        // The default is to have the 'File' and 'Help' menus only;
        //   let the specific cases below make any needed addition.
        //-----------------------------------------

        branchEditorMenu.setVisible(false);
        eventsMenu.setVisible(false);
        viewsMenu.setVisible(false);
        notesMenu.setVisible(false);
        todolistsMenu.setVisible(false);
        searchesMenu.setVisible(false);

        MemoryBank.debug("Setting MenuBar Configuration: " + theCurrentContext);

        switch (theCurrentContext) {
            case "Year View":  // Year View
            case "Month View":  // Month View
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
            case "Consolidated View":
            case "Upcoming Events Branch Editor":  // Upcoming Events
            case "To Do Lists Branch Editor":  // TodoBranchHelper
                branchEditorMenu.setVisible(true);
                break;
            case "Upcoming Event":
                eventsMenu.setVisible(true);
                break;
            case "To Do List":       // A List
                todolistsMenu.setVisible(true);
                break;
            default: // No special handling but still a valid configuration.
                // Expected examples:  About, Branches without an editor.
                // Unexpected - will only show up during development.
                break;
        }
    } // end manageMenus
} // end class AppMenuBar