import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

// (Eventually there may be a NoteGroupAccessor interface that NoteGroupFile will implement,
//  and we could alternatively use NoteGroupDatabase that also implements it).
//  NoteGroupData --> NoteGroupFile --> NoteGroupPanel
//  NoteGroupData --> NoteGroupAccessor --> NoteGroupPanel


class NoteGroupFile extends NoteGroupData {
    static String basePath;

    private String groupFilename; // Access with getGroupFilename() & setGroupFilename()

    static {
        // We give this string a trailing separatorChar because we really
        // do want a 'base' path as opposed to a complete path.  The common usage
        // is to build on it to make a final path to a lower level, but occasionally
        // it does get used by itself, and the trailing separatorChar causes no problems.
        basePath = MemoryBank.userDataHome + File.separatorChar;
    }


    protected boolean deleteFile(File f) {
        // There are a couple of cases where we could try to delete a file that is not there
        // in the first place.  So - we verify that the file is really there and only if it is
        // do we try to delete it.  Then we check for existence again because the deletion can
        // occasionally return a false negative (because of gc/timing issues?).
        // But File.delete() does not ever return a false positive (that I've seen).
        // With this sandwiched condition, the user is only notified of a problem if the file still exists.
        if (f.exists() && !f.delete() && f.exists()) {
            (new Exception("File removal exception!")).printStackTrace();
            NoteGroupPanel.ems = "Error - unable to remove: " + f.getName();
            NoteGroupPanel.optionPane.showMessageDialog(null,
                    NoteGroupPanel.ems, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            return true;
        } // end if
    } // end deleteFile

    static String getFullFilename(String areaName, String prettyName) {
        String prefix = "";
        switch (areaName) {
            case "Goals":
                prefix = GoalGroupPanel.filePrefix;
                break;
            case "UpcomingEvents":
                prefix = EventNoteGroupPanel.filePrefix;
                break;
            case "TodoLists":
                prefix = TodoNoteGroupPanel.filePrefix;
                break;
            case "SearchResults":
                prefix = SearchResultGroupPanel.filePrefix;
                break;
        }
        return basePath + areaName + File.separatorChar + prefix + prettyName + ".json";
    }

    public String getGroupFilename() {
        return groupFilename;
    }


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
        }// end try/catch
        return theGroup;
    }

    // A convenience method so that an instance can make a call to the static method
    //   without having to provide the parameter.
    String prettyName() {
        return prettyName(groupFilename);
    } // end prettyName


    //-----------------------------------------------------------------
    // Method Name:  prettyName
    //
    // A formatter for a String that is a filename specifier.  It strips
    //   away the File path, separators, prefix and ending, leaving only
    //   the base (pretty) name of the file.  It works left-to-right, as
    //   the input shrinks (in case that's important for you to know).
    // Usage is intended for data files of non-Calendar NoteGroups;
    //   Calendar NoteGroups use a different 'name' and do not need
    //   to prettify the path+name of their data file since it is not
    //   intended to be shown to users.
    //-----------------------------------------------------------------
    static String prettyName(String theLongName) {
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

        // Cut off the leading group type (event_, todo_, search_, etc)
        i = thePrettyName.indexOf("_");
        if (i >= 0) thePrettyName = thePrettyName.substring(i + 1);

        // Drop the JSON file extension
        i = thePrettyName.lastIndexOf(".json");
        if (i > 0) thePrettyName = thePrettyName.substring(0, i);

        return thePrettyName;
    } // end prettyName


    // Write the Group data to a file.  Provide the full path and filename, including extension.
    static int saveGroupData(String theFilename, Object[] theGroup) {
        int notesWritten = 0;
        BufferedWriter bw = null;
        Exception e = null;
        try {
            FileOutputStream fileStream = FileUtils.openOutputStream(new File(theFilename)); // Creates parent directories, if needed.
            OutputStreamWriter writer = new OutputStreamWriter(fileStream, StandardCharsets.UTF_8);
            bw = new BufferedWriter(writer);
            bw.write(AppUtil.toJsonString(theGroup));
            // Set the number of notes written, only AFTER the write.
            notesWritten = ((List) theGroup[theGroup.length - 1]).size();
        } catch (Exception ex) {
            // This is a catch-all for other problems that may arise, such as finding a subdirectory of the
            // same name in the directory where you want to put the file, or not having write permission.
            e = ex;
        } finally {
            if (e != null) {
                // This one may have been ignorable; print the message and see.
                System.out.println("Exception in NoteGroupFile.saveGroupData: \n  " + e.getMessage());
            } // end if there was an exception
            try {
                if (bw != null) {
                    // These flush/close lines may seem like overkill, but there is internet support for being so cautious.
                    bw.flush();
                    bw.close(); // Also closes the wrapped FileWriter
                }
            } catch (Exception ex) { // This one would be more serious - raise a 'louder' alarm.
                // Most likely would be an IOException, but we catch them all, to be sure.
                ex.printStackTrace(System.out);
            } // end try/catch
        } // end try/catch

        return notesWritten;
    }

    void setGroupFilename(String newName) {
        groupFilename = newName;
    }
}
