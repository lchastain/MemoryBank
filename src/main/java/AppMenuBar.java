import javax.swing.*;

public class AppMenuBar extends JMenuBar{
    static final long serialVersionUID = 1L; // JMenuBar wants this but we will not serialize.

    private static JMenu menuEditDay;
    private static JMenu menuEditMonth;
    private static JMenu menuEditTodo;
    private static JMenu menuEditYear;
    private static JMenu menuFile;
    private static JMenu menuFileSearchResult;
    private static JMenu menuFileTodoBranch;
    private static JMenu menuFileTodo;
    private static JMenu menuView;
    private static JMenu menuViewDate;
    private static JMenu menuHelp;

    static {
        menuFile = new JMenu("File");
        menuFile.add(new JMenuItem("Search..."));
        menuFile.add(new JMenuItem("Exit"));

        menuFileSearchResult = new JMenu("File");
        menuFileSearchResult.add(new JMenuItem("Close"));
        menuFileSearchResult.add(new JMenuItem("Search..."));
        menuFileSearchResult.add(new JMenuItem("Search these results..."));
        menuFileSearchResult.add(new JMenuItem("Review..."));
        menuFileSearchResult.add(new JMenuItem("Exit"));

        menuFileTodoBranch = new JMenu("File");
        menuFileTodoBranch.add(new JMenuItem("Add New List..."));
        menuFileTodoBranch.add(new JMenuItem("Search..."));
        menuFileTodoBranch.add(new JMenuItem("Exit"));

        menuFileTodo = new JMenu("File");
        menuFileTodo.add(new JMenuItem("Add New List..."));
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
        menuView.add(new JMenuItem("Refresh"));

        menuViewDate = new JMenu("View");
        menuViewDate.add(new JMenuItem("Today"));
        menuViewDate.add(new JMenuItem("Refresh"));

        menuHelp = new JMenu("Help");
        menuHelp.add(new JMenuItem("Contents"));
        menuHelp.add(new JMenuItem("About"));


        // Initial visibility of all menu items is false.
        // That can change in 'manageMenus'
        menuEditDay.setVisible(false);
        menuEditMonth.setVisible(false);
        menuEditTodo.setVisible(false);
        menuEditYear.setVisible(false);
        menuFileTodoBranch.setVisible(false);
        menuFileTodo.setVisible(false);
        menuFileSearchResult.setVisible(false);
        menuView.setVisible(false);
        menuViewDate.setVisible(false);
    } // end static

    public AppMenuBar() {
        super();
        add(menuFile);
        add(menuFileSearchResult);
        add(menuFileTodoBranch);
        add(menuFileTodo);
        add(menuEditDay);
        add(menuEditMonth);
        add(menuEditYear);
        add(menuEditTodo);
        add(menuView);
        add(menuViewDate);

        // This puts the 'Help' on the far right side.
        add(Box.createHorizontalGlue());
        add(menuHelp);
        // mb.setHelpMenu(menuHelp);  // Not implemented in Java 1.4.2 ...
        // Still not implemented in Java 1.5.0_03
        // In Java 1.8, throws a Not Implemented exception
    }

    // Given a string to indicate what 'mode' we are in,
    // display the menus that are appropriate to that mode.
    void manageMenus(String strMenuType) {
        // Set a default of having the 'File' and 'View' menus only;
        //   let the specific cases below make any needed alterations.
        //-----------------------------------------
        menuEditDay.setVisible(false);
        menuEditMonth.setVisible(false);
        menuEditTodo.setVisible(false);
        menuEditYear.setVisible(false);
        menuFile.setVisible(true);
        menuFileTodoBranch.setVisible(false);
        menuFileTodo.setVisible(false);
        menuFileSearchResult.setVisible(false);
        menuView.setVisible(true);
        menuViewDate.setVisible(false);

        switch (strMenuType) {
            case "Views":  // Not a lot of choices for a branch
            case "Notes":
                menuView.setVisible(false);
                break;
            case "Year View":  // Year View
                menuView.setVisible(false);
                menuViewDate.setVisible(true);
                break;
            case "Month Notes":  // Month Notes
                menuEditMonth.setVisible(true);
                menuView.setVisible(false);
                menuViewDate.setVisible(true);
                break;
            case "Month View":  // Month View
                menuView.setVisible(false);
                menuViewDate.setVisible(true);
                break;
            case "Day Notes":  // Day Notes
                menuEditDay.setVisible(true);
                menuView.setVisible(false);
                menuViewDate.setVisible(true);
                break;
            case "Search Result":  // Search Results
                menuFile.setVisible(false);
                menuFileSearchResult.setVisible(true);
                menuView.setVisible(false);
                break;
            case "Year Notes":  // Year Notes
                menuEditYear.setVisible(true);
                menuView.setVisible(false);
                menuViewDate.setVisible(true);
                break;
            case "Upcoming Events":  // Upcoming Events
                break;
            case "To Do Lists":  // TodoBranchHelper
                menuFile.setVisible(false);
                menuFileTodoBranch.setVisible(true);
                break;
            case "To Do List":  // A List
                menuFile.setVisible(false);
                menuFileTodo.setVisible(true);
                menuEditTodo.setVisible(true);
                break;
        }
    } // end manageMenus
} // end class AppMenuBar