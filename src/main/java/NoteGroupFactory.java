import java.io.File;

// This Factory has methods to construct and return supported NoteGroup types.

class NoteGroupFactory {
    private static String areaName;

    // This method will return the requested NoteGroup only if a file for
    // it exists; otherwise it returns null.  It is a better alternative
    // to simply calling a constructor, which of course cannot return a null.
    static NoteGroup loadGroup(String groupName, String filename) {
        if (groupName.startsWith("Goal")) {
            areaName = GoalGroup.areaName;
            if (exists(filename)) {
                MemoryBank.debug("Loading " + filename + " from filesystem");
                return new GoalGroup(filename);
            } // end if there is a file
        } else if (groupName.startsWith("Upcoming Event")) {
            areaName = EventNoteGroup.areaName;
            if (exists(filename)) {
                MemoryBank.debug("Loading " + filename + " from filesystem");
                return new EventNoteGroup(filename);
            } // end if there is a file
        } else if (groupName.startsWith("To Do List")) {
            areaName = TodoNoteGroup.areaName;
            if (exists(filename)) {
                MemoryBank.debug("Loading " + filename + " from filesystem");
                return new TodoNoteGroup(filename);
            } // end if there is a file
        } else if (groupName.startsWith("Search Result")) {
            areaName = SearchResultGroup.areaName;
            if (exists(filename)) {
                MemoryBank.debug("Loading " + filename + " from filesystem");
                return new SearchResultGroup(filename);
            } // end if there is a file
        }
        return null;
    }

    // Use this method if you want to get the group whether it has a data file or not.
    static NoteGroup loadOrMakeGroup(String theContext, String filename) {
        NoteGroup theGroup = loadGroup(theContext, filename);
        if (theGroup != null) return theGroup;

        // theContext is set by the AppMenuBar and is sent here by the menubar handler.
        // If we ever support some other way of getting here, that may need to change.
        if (theContext.startsWith("To Do List")) {
            return new TodoNoteGroup(filename);
        } else if (theContext.startsWith("Goal")) {
            return new GoalGroup(filename);
        } else if (theContext.startsWith("Upcoming Event")) {
            return new EventNoteGroup(filename);
        } else if (theContext.startsWith("Search Result")) {
            MemoryBank.debug("ERROR!  We do not make new Search Results with the Factory");
        }
        return null; // This line is only reached for unsupported group types.
    }

    private static boolean exists(String shortName) {
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
            fullFilename = FileGroup.getFullFilename(areaName, shortName);
        }

        return new File(fullFilename).exists();
    }

}
