import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

//-------------------------------------------------------------------------
// Class Name:  AppOptions
//
// The purpose of this class is to preserve the current state of the
//   application - JTree expanded nodes, variable leaf names, current
//   view, etc.
//
// Considered for storage but not implemented:
// 1.  A custom icon for the app
//     Not that useful for the trouble; a true user will be using a browser.
//     Also, do not want to proliferate different appearances of this somewhat
//     critical component; it could confuse support people, when we get to the
//     point of having support people.
//-------------------------------------------------------------------------
@SuppressWarnings("rawtypes")
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

    @JsonIgnore
    String consolidatedEventsViewName; // No longer going to use this..

    AppOptions() {
        goalsExpanded = false;
        eventsExpanded = false;
        viewsExpanded = false;
        notesExpanded = false;
        todoListsExpanded = false;
        searchesExpanded = false;
        theSelection = null;
        theSelectionRow = -1;
        consolidatedEventsViewName = "Consolidated View"; // Leaving this until it is no longer in the user data.
        goalsList = new Vector<>(0, 1);
        eventsList = new Vector<>(0, 1);
        tasksList = new Vector<>(0, 1);
        searchResultList = new Vector<>(0, 1);
    } // end constructor

    boolean active(GroupType groupType, String groupName) {
        Vector theList = null;
        switch (groupType) {
            case DAY_NOTES:
            case MONTH_NOTES:
            case YEAR_NOTES:
                // These types are always active.
                return true;
            case GOALS:
                theList = goalsList;
                break;
            case EVENTS:
                theList = eventsList;
                break;
            case TODO_LIST:
                theList = tasksList;
                break;
            case SEARCH_RESULTS:
                theList = searchResultList;
                break;
        }
        if(theList == null) return false;

        for (Object o : theList) {
            String object = (String) o;
            if(groupName.equals(object)) return true;
        }
        return false;
    }

    static void loadOpts() {
        Exception e = null;
        String filename = MemoryBank.userDataHome + File.separatorChar + "appOpts.json";

        try {
            String text = FileUtils.readFileToString(new File(filename), StandardCharsets.UTF_8.name());
            MemoryBank.appOpts = AppUtil.mapper.readValue(text, AppOptions.class);
            MemoryBank.debug("appOpts from JSON file: " + AppUtil.toJsonString(MemoryBank.appOpts));
        } catch (FileNotFoundException fnfe) {
            // not a problem; use defaults.
            MemoryBank.debug("User tree options not found; using defaults");
        } catch (IOException ioe) {
            e = ioe;
            e.printStackTrace();
        }

        if (e != null) {
            String ems = "Error in loading " + filename + " !\n";
            ems = ems + e.toString();
            ems = ems + "\nOptions load operation aborted.";
            MemoryBank.optionPane.showMessageDialog(null,
                    ems, "Error", JOptionPane.ERROR_MESSAGE);
        } // end if
    } // end loadOpts

    static void saveOpts() {
        String filename = MemoryBank.userDataHome + File.separatorChar + "appOpts.json";
        MemoryBank.debug("Saving application option data in " + filename);

        try (FileWriter writer = new FileWriter(filename);
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(AppUtil.toJsonString(MemoryBank.appOpts));
            bw.flush();
        } catch (IOException ioe) {
            // Since saveOpts is to be called via a shutdown hook that is not going to
            // wait around for the user to 'OK' an error dialog, any error in saving
            // will only be reported in a printout via MemoryBank.debug because
            // otherwise the entire process will hang up waiting for the user's 'OK'
            // on the dialog that will NOT be showing.

            // A normal user will not see the debug error printout but
            // they will most likely see other popups such as filesystem full, access
            // denied, etc, that a sysadmin type can resolve for them, that will
            // also fix this issue.
            String ems = ioe.getMessage();
            ems = ems + "\nMemory Bank options save operation aborted.";
            MemoryBank.debug(ems);
            // This popup caused a hangup and the vm had to be 'kill'ed.
            // JOptionPane.showMessageDialog(null,
            //    ems, "Error", JOptionPane.ERROR_MESSAGE);
            // Yes, even though the parent was null.
        } // end try/catch
    } // end saveOpts
} // end class AppOptions
