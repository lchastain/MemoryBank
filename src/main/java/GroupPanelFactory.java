import java.io.File;
import java.time.LocalDate;

// This Factory has methods to construct and return Panels of supported NoteGroup types.

class GroupPanelFactory {
    // Currently we only have the NoteGroupFile class as a usable NoteGroupDataAccessor,
    // so there is no 'switch' logic; for now we will just use it to
    // facilitate a file-based load of the data during the construction of a Panel.
    static NoteGroupPanel loadNoteGroupPanel(GroupInfo groupInfo) {
        String parentNodeString = "Nada";
        CalendarNoteGroupPanel calendarNoteGroupPanel = null;
        switch(groupInfo.groupType) {
            case YEAR_NOTES:
                calendarNoteGroupPanel = new YearNoteGroupPanel();
                break;
            case MONTH_NOTES:
                calendarNoteGroupPanel = new MonthNoteGroupPanel();
                break;
            case DAY_NOTES:
                calendarNoteGroupPanel = new DayNoteGroupPanel();
                break;
            case TODO_LIST:
                parentNodeString = "To Do Lists";
                break;
            case EVENTS:
                parentNodeString = "Upcoming Events";
                break;
            case GOALS:
                parentNodeString = "Goals";
                break;
            case SEARCH_RESULTS:
                parentNodeString = "Search Results";
                break;
        }
        if(calendarNoteGroupPanel != null) {
            LocalDate localDate = CalendarNoteGroup.getDateFromGroupName(groupInfo);
            calendarNoteGroupPanel.setDate(localDate);
            return calendarNoteGroupPanel;
        }
        return loadNoteGroup(parentNodeString, groupInfo.getGroupName());
    }


    // At some point you need to alter this methodology to adapt to usage of the
    // NoteGroupDataAccessor interface.  loadNoteGroup should not only apply to FILEs.
    // But now, a Panel does implement the interface, so it somewhat qualifies.  Streamlining
    //   here is not entirely done but this will work for now.
    //
    // This method will return the requested Panel only if it was previously
    // constructed and persisted; otherwise it returns null.  It is a better alternative
    // to simply calling a constructor, which of course cannot return a null.
    static NoteGroupPanel loadNoteGroup(String parentNodeString, String nodeString) {
        if (parentNodeString.startsWith("Goal")) {
            if (exists(GoalGroupPanel.areaName, nodeString)) {
                MemoryBank.debug("Loading " + nodeString + " from filesystem");
                return new GoalGroupPanel(nodeString);
            } // end if there is a file
        } else if (parentNodeString.startsWith("Upcoming Event")) {
            if (exists(EventNoteGroupPanel.areaName, nodeString)) {
                MemoryBank.debug("Loading " + nodeString + " from filesystem");
                return new EventNoteGroupPanel(nodeString);
            } // end if there is a file
        } else if (parentNodeString.startsWith("To Do List")) {
            if (exists(TodoNoteGroupPanel.areaName, nodeString)) {
                MemoryBank.debug("Loading " + nodeString + " from filesystem");
                return new TodoNoteGroupPanel(nodeString);
            } // end if there is a file
        } else if (parentNodeString.startsWith("Search Result")) {
            if (exists(SearchResultGroupPanel.areaName, nodeString)) {
                MemoryBank.debug("Loading " + nodeString + " from filesystem");
                return new SearchResultGroupPanel(nodeString);
            } // end if there is a file
        }
        return null;
    }



    // Use this method if you want to get the group whether it has persisted data or not.
    static NoteGroupPanel loadOrMakeGroup(String theContext, String nodeName) {
        NoteGroupPanel theGroup = loadNoteGroup(theContext, nodeName);
        if (theGroup != null) return theGroup;

        // theContext is set by the AppMenuBar and is sent here by the menubar handler.
        // If we ever support some other way of getting here, that may need to change.
        if (theContext.startsWith("To Do List")) {
            return new TodoNoteGroupPanel(nodeName);
        } else if (theContext.startsWith("Goal")) {
            return new GoalGroupPanel(nodeName);
        } else if (theContext.startsWith("Upcoming Event")) {
            return new EventNoteGroupPanel(nodeName);
        } else if (theContext.startsWith("Search Result")) {
            MemoryBank.debug("ERROR!  We do not make new Search Results with the Factory");
        }
        return null; // This line is only reached for unsupported group types.
    }

    private static boolean exists(String areaName, String shortName) {
        String fullFilename;

        int i = shortName.lastIndexOf(File.separatorChar);
        int j = shortName.lastIndexOf('/');
        int k = Math.max(i, j);

        if (k >= 0) { // if it has the File separator character
            // Then we assume (rightly or not) that the calling context has sent in
            // a path+filename, and that it is the correct full-blown File specifier
            // so that further grooming is unnecessary.
            fullFilename = shortName;
        } else {
            fullFilename = NoteGroupFile.makeFullFilename(areaName, shortName);
        }

        return new File(fullFilename).exists();
    }

}
