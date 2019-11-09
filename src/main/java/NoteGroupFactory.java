import java.io.File;

// This Factory has methods to construct and return supported NoteGroup types.

class NoteGroupFactory {
    private static String areaName;

    // This method will return the requested NoteGroup only if a file for
    // it exists; otherwise it returns null.  It is a better alternative
    // to simply calling a constructor, which of course cannot return a null.
    static NoteGroup getGroup(String groupType, String filename) {
        if (groupType.startsWith("Upcoming Event")) {
            areaName = EventNoteGroup.areaName;
            if (exists(filename)) {
                MemoryBank.debug("Loading " + filename + " from filesystem");
                return new EventNoteGroup(filename);
            } // end if there is a file
            return null;
        } else if (groupType.startsWith("To Do List")) {
            areaName = TodoNoteGroup.areaName;
            if (exists(filename)) {
                MemoryBank.debug("Loading " + filename + " from filesystem");
                return new TodoNoteGroup(filename);
            } // end if there is a file
            return null;
        } else if (groupType.startsWith("Search Result")) {
            areaName = SearchResultGroup.areaName;
            if (exists(filename)) {
                MemoryBank.debug("Loading " + filename + " from filesystem");
                return new SearchResultGroup(filename);
            } // end if there is a file
            return null;
        }
        return null;
    }

    // Use this method if you want to get the group whether it has a data file or not.
    static NoteGroup getOrMakeGroup(String theContext, String filename) {
        NoteGroup theGroup = getGroup(theContext, filename);
        if (theGroup != null) return theGroup;

        // theContext is set by the AppMenuBar and is sent here by the menubar handler.
        // If we ever support some other pathway, that may need to change.
        if (theContext.startsWith("To Do List")) {
            return new TodoNoteGroup(filename);
        } else if (theContext.startsWith("Upcoming Event")) {
            return new EventNoteGroup(filename);
        } else if (theContext.startsWith("Search Result")) {
            MemoryBank.debug("ERROR!  We do not make new Search Results with the factory");
        }
        return null; // This line is only reached for unsupported group types.
    }

    private static boolean exists(String shortName) {
        String fullFileName = getFullFilename(shortName);
        return new File(fullFileName).exists();
    }

    private static String getFullFilename(String theName) {
        String theFullFilename = null;
        int sepCharIndex = theName.lastIndexOf(File.separatorChar);
        if (sepCharIndex > 0) { // The calling context sent in a path+filename.
            // We now assume (correctly or not) that it's already the full-blown specifier.
            theFullFilename = theName;
        } else { // theName is just the filename only, without a path.
            switch (areaName) {
                case "UpcomingEvents":
                    theFullFilename = NoteGroup.basePath(areaName) + "event_" + theName + ".json";
                    break;
                case "TodoLists":
                    theFullFilename = NoteGroup.basePath(areaName) + "todo_" + theName + ".json";
                    break;
                case "SearchResults":
                    theFullFilename = NoteGroup.basePath(areaName) + "search_" + theName + ".json";
                    break;
            }
        }
        return theFullFilename;
    }


}
