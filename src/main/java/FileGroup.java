import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public abstract class FileGroup {
    static String basePath;

    // Status report codes for Load / Save
    static final int INITIAL = 500;
    static final int ONGOING = 501;
    static final int DELETEOLDFILEFAILED = 502;
    static final int DIRECTORYINMYPLACE = 503;
    static final int CANNOTMAKEAPATH = 504;
    static final int FILEINMYDIRPATH = 505;
    static final int OTHERFAILURE = 506;
    static final int SUCCEEDED = 600;

    // Directions for Sort operations
    static final int ASCENDING = 0;
    static final int DESCENDING = 1;

    UUID theId;
    private String theName;
    private String groupFilename; // Access with getGroupFilename() & setGroupFilename()
    boolean groupChanged;  // Flag used to determine if saving data might be necessary.

    static {
        // We return with a trailing separatorChar because we really
        // do want a base path as opposed to a final / complete path.
        basePath = MemoryBank.userDataHome + File.separatorChar;
    }

    static String getFullFilename(String areaName, String prettyName) {
        String prefix = "";
        switch (areaName) {
            case "Goals":
                prefix = "goal_";
                break;
            case "UpcomingEvents":
                prefix = EventNoteGroup.filePrefix;
                break;
            case "TodoLists":
                prefix = TodoNoteGroup.filePrefix;
                break;
            case "SearchResults":
                prefix = SearchResultGroup.filePrefix;
                break;
        }
        return basePath + areaName + File.separatorChar + prefix + prettyName + ".json";
    }

    // ---------------------------------------------------------------------------------
    // Method Name: loadGroupData
    //
    // This is a static data loader that helps separate the loading of the data
    //   from the various components and methods that act upon it.
    // ---------------------------------------------------------------------------------
    static Object[] loadFileData(String theFilename) {
        // theFilename string needs to be the full path to the file.
        return loadFileData(new File(theFilename));
    }

    static Object[] loadFileData(File theFile) {
        Object[] theGroup = null;
        try {
            String text = FileUtils.readFileToString(theFile, StandardCharsets.UTF_8.name());
            theGroup = AppUtil.mapper.readValue(text, Object[].class);
            //System.out.println("Group data from JSON file: " + AppUtil.toJsonString(theGroup));
        } catch (FileNotFoundException fnfe) { // This is allowed, but you get back a null.
        } catch (IOException ex) {
            ex.printStackTrace();
        } // end try/catch
        return theGroup;
    }

    public String getGroupFilename() {
        return groupFilename;
    }// end getGroupFilename

    // Previously we used the getName/setName that is built into a JComponent.  Now that JComponent
    // is no longer in the direct hierarchy of a NoteGroup, we need to provide our own versions.
    String getName() {
        return theName;
    }

    // Called to prompt implementations to save their data before they go away.
    abstract void preClose();

    //-----------------------------------------------------------------
    // Method Name:  prettyName
    //
    // A formatter for the full filename specifier.  It strips away
    //   the File path, separators, prefix and ending, leaving only
    //   the base (pretty) name of the file.
    // Note that this method name was chosen so as to not conflict
    //   with the 'getName' method of the Component class.
    // Usage is intended for data files of non-Calendar NoteGroups;
    //   Calendar NoteGroups use a different 'name' and do not need
    //   to prettify the path+name of their data file.
    //-----------------------------------------------------------------
    String prettyName() {
        return prettyName(groupFilename);
    } // end prettyName


    //-----------------------------------------------------------------
    // Method Name:  prettyName
    //
    // A formatter for a String that is a filename specifier.  It strips
    //   away the File path, separators, prefix and ending, leaving only
    //   the base (pretty) name of the file.
    //-----------------------------------------------------------------
    String prettyName(String theLongName) {
        // Trim any leading/trailing whitespace.
        String thePrettyName = theLongName.trim();

        // Find the final path separator character.
        // On a Windows system the separator char will be '\' but
        // Java will also understand and can work with '/'.  Here,
        // we look for both, which might be the same thing on a
        // unix system, but this works even if the input is mixed.
        int i = thePrettyName.lastIndexOf(File.separatorChar);
        int j = thePrettyName.lastIndexOf('/');
        int k = Math.max(i, j);

        // Cut off the leading path specifier characters, if present.
        if (k >= 0) { // if it has the File separator character
            // then we only want the part after that
            thePrettyName = theLongName.substring(k + 1);
        }

        // Drop the JSON file extension
        i = thePrettyName.lastIndexOf(".json");
        if (i > 0) thePrettyName = thePrettyName.substring(0, i);

        // Cut off the leading group type (event_, todo_, search_, etc)
        i = thePrettyName.indexOf("_");
        if (i >= 0) thePrettyName = thePrettyName.substring(i + 1);

        return thePrettyName;
    } // end prettyName

    //----------------------------------------------------------------
    // Method Name: setGroupChanged
    //
    // Called by all contexts that make a change to the data, each
    //   time a change is made.  Child classes can override if they
    //   need to intercept a state change, but in that case they
    //   should still call this super method so that group saving
    //   is done when needed.
    //----------------------------------------------------------------
    void setGroupChanged(boolean b) {
        groupChanged = b;
    } // end setGroupChanged

    // This is the (short) name of the group; not always the short version of the data file name.
    void setName(String newName) {
        if(newName == null) theName = "";
        else theName = newName.trim();
    }

    void setGroupFilename(String theName) {
        groupFilename = theName;
    }
}