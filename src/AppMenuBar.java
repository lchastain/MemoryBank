import javax.swing.*;

public class AppMenuBar extends JMenuBar{
    static final long serialVersionUID = 1L;

    private static JMenu menuEditDay;
    private static JMenu menuEditMonth;
    private static JMenu menuEditTodo;
    private static JMenu menuEditYear;
    private static JMenu menuFile;
    private static JMenu menuFileSearchResult;
    private static JMenu menuFileTodo;
    private static JMenu menuView;
    private static JMenu menuViewEvent;
    private static JMenu menuViewDate;
    private static JMenu menuHelp;

    static {
        menuFile = new JMenu("File");
        menuFile.add(new JMenuItem("Search..."));
        menuFile.add(new JMenuItem("Export"));
        menuFile.add(new JMenuItem("Exit"));

        menuFileSearchResult = new JMenu("File");
        menuFileSearchResult.add(new JMenuItem("Close"));
        menuFileSearchResult.add(new JMenuItem("Search..."));
        menuFileSearchResult.add(new JMenuItem("Search these results..."));
        menuFileSearchResult.add(new JMenuItem("Review..."));
        menuFileSearchResult.add(new JMenuItem("Exit"));

        menuFileTodo = new JMenu("File");
        menuFileTodo.add(new JMenuItem("Search..."));
        menuFileTodo.add(new JMenuItem("Merge..."));
        menuFileTodo.add(new JMenuItem("Print..."));
        menuFileTodo.add(new JMenuItem("Save As..."));
        menuFileTodo.add(new JMenuItem("Exit"));

        menuEditDay = new JMenu("Edit");
        menuEditDay.add(new JMenuItem("undo"));
        menuEditDay.add(new JMenuItem("Clear Day"));

        menuEditMonth = new JMenu("Edit");
        menuEditMonth.add(new JMenuItem("undo"));
        menuEditMonth.add(new JMenuItem("Clear Month"));

        menuEditYear = new JMenu("Edit");
        menuEditYear.add(new JMenuItem("undo"));
        menuEditYear.add(new JMenuItem("Clear Year"));

        menuEditTodo = new JMenu("Edit");
        menuEditTodo.add(new JMenuItem("undo"));
        menuEditTodo.add(new JMenuItem("Clear Entire List"));
        menuEditTodo.addSeparator();
        menuEditTodo.add(new JMenuItem("Set Options..."));

        menuView = new JMenu("View");
        menuView.add(new JMenuItem("Set Look and Feel..."));

        menuViewEvent = new JMenu("View");
        // Don't leave Today here...
        menuViewEvent.add(new JMenuItem("Today"));
        // menuViewEvent.add(new JMenuItem("Date Format"));
        menuViewEvent.add(new JMenuItem("Refresh"));
        menuViewEvent.add(new JMenuItem("Set Look and Feel..."));

        menuViewDate = new JMenu("View");
        menuViewDate.add(new JMenuItem("Today"));
        menuViewDate.add(new JMenuItem("Set Look and Feel..."));

        menuHelp = new JMenu("Help");
        menuHelp.add(new JMenuItem("Contents"));
        menuHelp.add(new JMenuItem("About"));


        // Initial visibility of all menu items is false.
        // That can change in 'manageMenus'
        menuEditDay.setVisible(false);
        menuEditMonth.setVisible(false);
        menuEditTodo.setVisible(false);
        menuEditYear.setVisible(false);
        menuFileTodo.setVisible(false);
        menuFileSearchResult.setVisible(false);
        menuView.setVisible(false);
        menuViewEvent.setVisible(false);
        menuViewDate.setVisible(false);
    } // end static

    public AppMenuBar() {
        super();
        add(menuFile);
        add(menuFileSearchResult);
        add(menuFileTodo);
        add(menuEditDay);
        add(menuEditMonth);
        add(menuEditYear);
        add(menuEditTodo);
        add(menuView);
        add(menuViewEvent);
        add(menuViewDate);

        // This puts the 'Help' on the far right side.
        add(Box.createHorizontalGlue());
        add(menuHelp);
        // mb.setHelpMenu(menuHelp);  // Not implemented in Java 1.4.2 ...
        // Still not implemented in Java 1.5.0_03
    }

    // Given a string to indicate what 'mode' we are in,
    // display the menus that are appropriate to that mode.
    public void manageMenus(String strMenuType) {
        // Set a default of having the 'File' and 'View' menus only;
        //   let the specific cases below make any needed alterations.
        //-----------------------------------------
        menuEditDay.setVisible(false);
        menuEditMonth.setVisible(false);
        menuEditTodo.setVisible(false);
        menuEditYear.setVisible(false);
        menuFile.setVisible(true);
        menuFileTodo.setVisible(false);
        menuFileSearchResult.setVisible(false);
        menuView.setVisible(true);
        menuViewEvent.setVisible(false);
        menuViewDate.setVisible(false);

        if (strMenuType.equals("Day Notes")) { // Day Notes
            menuEditDay.setVisible(true);
            menuView.setVisible(false);
            menuViewDate.setVisible(true);
        } else if (strMenuType.equals("Month Notes")) { // Month Notes
            menuEditMonth.setVisible(true);
            menuView.setVisible(false);
            menuViewDate.setVisible(true);
        } else if (strMenuType.equals("Month View")) { // Month View
            menuView.setVisible(false);
            menuViewDate.setVisible(true);
        } else if (strMenuType.equals("Year View")) { // Year View
            menuView.setVisible(false);
            menuViewDate.setVisible(true);
        } else if (strMenuType.equals("Search Result")) { // Search Results
            menuFile.setVisible(false);
            menuFileSearchResult.setVisible(true);
            menuView.setVisible(false);
            menuViewDate.setVisible(true); // Temporary; should go away.
        } else if (strMenuType.equals("Year Notes")) { // Year Notes
            menuEditYear.setVisible(true);
            menuView.setVisible(false);
            menuViewDate.setVisible(true);
        } else if (strMenuType.equals("Upcoming Events")) { // Upcoming Events
            menuView.setVisible(false);
            menuViewEvent.setVisible(true);
        } else if (strMenuType.equals("To Do List")) { // A List
            menuFile.setVisible(false);
            menuFileTodo.setVisible(true);
            menuEditTodo.setVisible(true);
        } // end if
    } // end manageMenus
} // end class AppMenuBar