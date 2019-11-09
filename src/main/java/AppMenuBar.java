import javax.swing.*;

public class AppMenuBar extends JMenuBar{
    static final long serialVersionUID = 1L; // JMenuBar wants this but we will not serialize.

    private static JMenu dayNoteEditMenu;
    private static JMenu monthEditMenu;
    private static JMenu todoEditMenu;
    private static JMenu yearEditMenu;
    private static JMenu fileMenu;
    private static JMenu searchResultFileMenu;
    private static JMenu todoBranchFileMenu;
    private static JMenu todoFileMenu;
    private static JMenu viewMenu;
    private static JMenu noteViewMenu;
    private static JMenu viewsViewMenu;
    private static JMenu helpMenu;
    private String theCurrentContext;

    static {
        //------------- File Menus -------------------------------------------------
        fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem("Search..."));
        fileMenu.add(new JMenuItem("Icon Manager..."));
        fileMenu.add(new JMenuItem("Exit"));

        searchResultFileMenu = new JMenu("File");
        searchResultFileMenu.add(new JMenuItem("Close"));
        searchResultFileMenu.add(new JMenuItem("Search..."));
        searchResultFileMenu.add(new JMenuItem("Review..."));
        searchResultFileMenu.add(new JMenuItem("Icon Manager..."));
        searchResultFileMenu.add(new JMenuItem("Exit"));

        todoBranchFileMenu = new JMenu("File");
        todoBranchFileMenu.add(new JMenuItem("Add New..."));
        todoBranchFileMenu.add(new JMenuItem("Search..."));
        todoBranchFileMenu.add(new JMenuItem("Icon Manager..."));
        todoBranchFileMenu.add(new JMenuItem("Exit"));

        todoFileMenu = new JMenu("File");
        todoFileMenu.add(new JMenuItem("Close"));
        todoFileMenu.add(new JMenuItem("Add New..."));
        todoFileMenu.add(new JMenuItem("Search..."));
        todoFileMenu.add(new JMenuItem("Icon Manager..."));
        todoFileMenu.add(new JMenuItem("Merge..."));
        todoFileMenu.add(new JMenuItem("Print..."));
        todoFileMenu.add(new JMenuItem("Save As..."));
        todoFileMenu.add(new JMenuItem("Exit"));

        //------------- Edit Menus -------------------------------------------------
        dayNoteEditMenu = new JMenu("Edit");
        dayNoteEditMenu.add(new JMenuItem("undo"));
        dayNoteEditMenu.add(new JMenuItem("Clear Day"));

        monthEditMenu = new JMenu("Edit");
        monthEditMenu.add(new JMenuItem("undo"));
        monthEditMenu.add(new JMenuItem("Clear Month"));

        yearEditMenu = new JMenu("Edit");
        yearEditMenu.add(new JMenuItem("undo"));
        yearEditMenu.add(new JMenuItem("Clear Year"));

        todoEditMenu = new JMenu("Edit");
        todoEditMenu.add(new JMenuItem("undo"));
        todoEditMenu.add(new JMenuItem("Clear Entire List"));
        todoEditMenu.addSeparator();
        todoEditMenu.add(new JMenuItem("Set Options..."));

        //------------- View Menus -------------------------------------------------
        viewMenu = new JMenu("View");
        viewMenu.add(new JMenuItem("Refresh"));

        viewsViewMenu = new JMenu("View");
        viewsViewMenu.add(new JMenuItem("Today"));

        noteViewMenu = new JMenu("View");
        noteViewMenu.add(new JMenuItem("Today"));
        noteViewMenu.add(new JMenuItem("Refresh"));

        //------------- Help Menu --------------------------------------------------
        helpMenu = new JMenu("Help");
        helpMenu.add(new JMenuItem("Contents"));
        helpMenu.add(new JMenuItem("About"));


        // Initial visibility of all menu items is false.
        // That can change in 'manageMenus'
        dayNoteEditMenu.setVisible(false);
        monthEditMenu.setVisible(false);
        todoEditMenu.setVisible(false);
        yearEditMenu.setVisible(false);
        todoBranchFileMenu.setVisible(false);
        todoFileMenu.setVisible(false);
        searchResultFileMenu.setVisible(false);
        viewMenu.setVisible(false);
        viewsViewMenu.setVisible(false);
        noteViewMenu.setVisible(false);
    } // end static

    public AppMenuBar() {
        super();
        add(fileMenu);
        add(searchResultFileMenu);
        add(todoBranchFileMenu);
        add(todoFileMenu);
        add(dayNoteEditMenu);
        add(monthEditMenu);
        add(yearEditMenu);
        add(todoEditMenu);
        add(viewMenu);
        add(viewsViewMenu);
        add(noteViewMenu);

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
    // display the menus that are appropriate to that mode.
    void manageMenus(String theContext) {
        theCurrentContext = theContext;

        // Set a default of having the 'File' and 'View' menus only;
        //   let the specific cases below make any needed alterations.
        //-----------------------------------------
        fileMenu.setVisible(true);
        todoBranchFileMenu.setVisible(false);
        todoFileMenu.setVisible(false);
        searchResultFileMenu.setVisible(false);
        dayNoteEditMenu.setVisible(false);
        monthEditMenu.setVisible(false);
        todoEditMenu.setVisible(false);
        yearEditMenu.setVisible(false);
        viewMenu.setVisible(true);
        viewsViewMenu.setVisible(false);
        noteViewMenu.setVisible(false);

        MemoryBank.debug("MenuBar Configuration: " + theCurrentContext);

        switch (theCurrentContext) {
            case "Year View":  // Year View
                viewMenu.setVisible(false);
                viewsViewMenu.setVisible(true);
                break;
            case "Month Notes":  // Month Notes
                monthEditMenu.setVisible(true);
                viewMenu.setVisible(false);
                noteViewMenu.setVisible(true);
                break;
            case "Month View":  // Month View
                viewMenu.setVisible(false);
                viewsViewMenu.setVisible(true);
                break;
            case "Day Notes":  // Day Notes
                dayNoteEditMenu.setVisible(true);
                viewMenu.setVisible(false);
                noteViewMenu.setVisible(true);
                break;
            case "Search Result":  // Search Results
                fileMenu.setVisible(false);
                searchResultFileMenu.setVisible(true);
                viewMenu.setVisible(false);
                break;
            case "Year Notes":  // Year Notes
                yearEditMenu.setVisible(true);
                viewMenu.setVisible(false);
                noteViewMenu.setVisible(true);
                break;
            case "Upcoming Events Branch Editor":  // Upcoming Events
            case "To Do Lists Branch Editor":  // TodoBranchHelper
                fileMenu.setVisible(false);
                todoBranchFileMenu.setVisible(true);
                viewMenu.setVisible(false);
                break;
            case "Upcoming Event":
            case "To Do List":       // A List
                fileMenu.setVisible(false);
                todoFileMenu.setVisible(true);
                todoEditMenu.setVisible(true);
                break;
            default: // No special handling but still a valid configuration.
                // Expected examples:  About, Branches.
                // Unexpected - will only show up during development.
                viewMenu.setVisible(false);
                break;
        }
    } // end manageMenus
} // end class AppMenuBar