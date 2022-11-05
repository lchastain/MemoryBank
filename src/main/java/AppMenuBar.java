import javax.swing.*;
import java.awt.event.ActionListener;

public class AppMenuBar extends JMenuBar {
    static final long serialVersionUID = 1L; // JMenuBar wants this but we will not serialize.

    // Checkbox menu item for app-level option:
    static JCheckBoxMenuItem groupCalendarNotes;

    // Menus
    //-------------------------------------------------
    private static final JMenu archiveMenu;
    private static final JMenu fileMenu;
    private static final JMenu branchEditorMenu;
    private static final JMenu deletedMenu;

    private static final JMenu goalsMenu;
    private static final JMenu eventsMenu;
    private static final JMenu viewsMenu;
    private static final JMenu calendarNotesMenu;
    private static final JMenu userNotesMenu;
    private static final JMenu todolistsMenu;
    private static final JMenu searchesMenu;

    private static final JMenu helpMenu;
    //-------------------------------------------------

    private String theCurrentContext;
    private boolean showDeleteUndo;
    private static final JMenuItem goBack = new JMenuItem("Go Back");
    static final JCheckBoxMenuItem reviewMode = new JCheckBoxMenuItem("Review Mode");

    static {
        groupCalendarNotes = new JCheckBoxMenuItem("Group Calendar Notes");
        groupCalendarNotes.setState(MemoryBank.appOpts.groupCalendarNotes);

        fileMenu = new JMenu("App");
        fileMenu.add(new JMenuItem("Search..."));
        fileMenu.add(new JMenuItem("Archive..."));
        fileMenu.add(groupCalendarNotes); // "Group Calendar Notes"
        fileMenu.add(new JMenuItem("Show Scheduled Events"));
        if(MemoryBank.appEnvironment.equals("ide")) {
            fileMenu.add(new JMenuItem("Show Current NoteGroup"));
            fileMenu.add(new JMenuItem("Show Keepers"));
        }
        fileMenu.add(new JMenuItem("Icon Manager..."));
        fileMenu.add(new JMenuItem("Exit"));

        branchEditorMenu = new JMenu("List");
        branchEditorMenu.add(new JMenuItem("Add New..."));

        deletedMenu = new JMenu("Deleted");
        deletedMenu.add(new JMenuItem("Undo Delete"));

        archiveMenu = new JMenu("Archive");
        archiveMenu.add("View");
        archiveMenu.add("Remove");

        goalsMenu = new JMenu("Goal");
        goalsMenu.add(new JMenuItem("Undo All"));
        goalsMenu.add(new JMenuItem("Close"));
        goalsMenu.add(new JMenuItem("Add New..."));
        goalsMenu.add(new JMenuItem("Save"));
        goalsMenu.add(new JMenuItem("Save As..."));
        goalsMenu.add(new JMenuItem("Clear All"));
        goalsMenu.add(new JMenuItem("Delete"));

        eventsMenu = new JMenu("Upcoming Event");
        eventsMenu.add(new JMenuItem("Undo All"));
        eventsMenu.add(new JMenuItem("Close"));
        eventsMenu.add(new JMenuItem("Add New..."));
        eventsMenu.add(new JMenuItem("Save"));
        eventsMenu.add(new JMenuItem("Save As..."));
        eventsMenu.add(new JMenuItem("Clear All"));
        eventsMenu.add(new JMenuItem("Delete"));

        viewsMenu = new JMenu("View");
        viewsMenu.add(new JMenuItem("Today"));

        calendarNotesMenu = new JMenu("Calendar Notes");
        calendarNotesMenu.add(new JMenuItem("Undo All"));
        calendarNotesMenu.add(reviewMode);
        calendarNotesMenu.add(new JMenuItem("Today"));
        calendarNotesMenu.add(new JMenuItem("Save"));
        calendarNotesMenu.add(new JMenuItem("Clear All"));

        userNotesMenu = new JMenu("Notes");
        userNotesMenu.add(new JMenuItem("Undo All"));
        userNotesMenu.add(new JMenuItem("Close"));
        userNotesMenu.add(new JMenuItem("Add New..."));
        userNotesMenu.add(new JMenuItem("Merge..."));
        //userNotesMenu.add(new JMenuItem("Print..."));
        userNotesMenu.add(new JMenuItem("Save"));
        userNotesMenu.add(new JMenuItem("Save As..."));
        userNotesMenu.add(new JMenuItem("Clear All"));
        userNotesMenu.add(new JMenuItem("Delete"));

        todolistsMenu = new JMenu("To Do List");
        todolistsMenu.add(new JMenuItem("Undo All"));
        todolistsMenu.add(new JMenuItem("Close"));
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
        add(fileMenu);
        add(branchEditorMenu);
        add(deletedMenu);
        add(archiveMenu);
        add(goalsMenu);
        add(eventsMenu);
        add(viewsMenu);
        add(calendarNotesMenu);
        add(userNotesMenu);
        add(todolistsMenu);
        add(searchesMenu);
        add(goBack);
        showDeleteUndo = false;

        // This puts the 'Help' on the far right side.
        add(Box.createHorizontalGlue());
        add(helpMenu);
        // mb.setHelpMenu(menuHelp);  // Not implemented in Java 1.4.2 ...
        // Still not implemented in Java 1.5.0_03
        // In Java 1.8, throws a Not Implemented exception
    }


    // Add a handler to all menu items.
    void addHandler(ActionListener actionListener) {
        //---------------------------------------------------------
        // Note - if you need cascading menus in the future, use
        //   the recursive version of this as implemented in
        //   LogPane.java, a now archived predecessor to AppTreePanel.
        //---------------------------------------------------------
        int numMenus = getMenuCount();
        // MemoryBank.debug("Number of menus found: " + numMenus);
        for (int i = 0; i < numMenus; i++) {
            JMenu jm = getMenu(i);
            if (jm == null) continue;

            for (int j = 0; j < jm.getItemCount(); j++) {
                JMenuItem jmi = jm.getItem(j);
                if (jmi == null) continue; // Separator
                jmi.addActionListener(actionListener);
            } // end for j
        } // end for i
        //---------------------------------------------------------
        goBack.addActionListener(actionListener);
    }

    String getCurrentContext() {
        return theCurrentContext;
    }

    // Get the menu that is appropriate for the selected tree node.
    // This is NOT the main handler; just answers the 'what menu was this?' question.
    static JMenu getNodeMenu(String selectionContext) {
        JMenu theMenu;
        switch (selectionContext) {
            case "Year View", "Month View", "Week View" -> theMenu = viewsMenu;
            case "Calendar Notes", "Day Notes", "Month Notes", "Year Notes" -> theMenu = calendarNotesMenu;
            case "Search Result" -> {
                theMenu = searchesMenu;
                searchesMenu.setVisible(true);
            }  // Upcoming Events
            case "Goals Branch Editor", "Upcoming Events Branch Editor", "To Do Lists Branch Editor" ->  // TodoBranchHelper
                    theMenu = branchEditorMenu;
            case "Goal" -> theMenu = goalsMenu;
            case "Upcoming Event" -> theMenu = eventsMenu;
            case "To Do List" -> theMenu = todolistsMenu;
            case "Notes" -> theMenu = userNotesMenu;
            default -> // No additional menu is defined for the specified node.
                    theMenu = null;
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
        archiveMenu.setVisible(false);
        goalsMenu.setVisible(false);
        eventsMenu.setVisible(false);
        viewsMenu.setVisible(false);
        calendarNotesMenu.setVisible(false);
        userNotesMenu.setVisible(false);
        todolistsMenu.setVisible(false);
        searchesMenu.setVisible(false);
        goBack.setVisible(false);

        deletedMenu.setVisible(showDeleteUndo);

        MemoryBank.debug("Setting MenuBar Configuration: " + theCurrentContext);

        switch (theCurrentContext) {
            case "One Archive" -> archiveMenu.setVisible(true);
            // Year View
            // Month View
            case "Year View", "Month View", "Week View" -> viewsMenu.setVisible(true);
            case "Calendar Notes", "Day Notes", "Month Notes", "Year Notes" -> calendarNotesMenu.setVisible(true);
            case "Search Result" ->  // Search Results
                    searchesMenu.setVisible(true);
            case "Goals Branch Editor" -> {
                branchEditorMenu.setText("Goals");
                branchEditorMenu.setVisible(true);
            }
            case "Upcoming Events Branch Editor" -> {  // Upcoming Events
                branchEditorMenu.setText("Events");
                branchEditorMenu.setVisible(true);
            }
            case "To Do Lists Branch Editor" -> {  // TodoBranchHelper
                branchEditorMenu.setText("To Do Lists");
                branchEditorMenu.setVisible(true);
            }
            case "Notes Branch Editor" -> {
                branchEditorMenu.setText("Notes");
                branchEditorMenu.setVisible(true);
            }
            case "Goal" -> goalsMenu.setVisible(true);
            case "Upcoming Event" -> eventsMenu.setVisible(true);
            case "Notes" -> userNotesMenu.setVisible(true);
            case "To Do List" -> todolistsMenu.setVisible(true);
            case "Viewing FoundIn" -> goBack.setVisible(true);
            default -> {
            } // aka "No Selection"
            // No special handling but still a valid configuration; all custom menus hidden.
            // Expected uses:  Show About, Branches without an editor, a few others.
        }
    } // end manageMenus

    void showRestoreOption(boolean showIt) {
        showDeleteUndo = showIt;
        deletedMenu.setVisible(showDeleteUndo);
    }

} // end class AppMenuBar