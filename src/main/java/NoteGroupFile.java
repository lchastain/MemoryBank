import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;


class NoteGroupFile extends NoteGroupData implements NoteGroupDataAccessor {
    static String basePath;
    protected GroupProperties myProperties; // All children can access this directly, but CNGPs use a getter.
    boolean saveWithoutData;  // This can allow for empty search results, brand new TodoLists, etc.
    boolean saveIsOngoing;

    private String failureReason; // Various file access failure reasons, or null.

    protected String groupFilename; // Access with getGroupFilename() & setGroupFilename()

    static {
        // We give this string a trailing separatorChar because we really
        // do want a 'base' path as opposed to a complete path.  The common usage
        // is to build on it to make a final path to a lower level, but occasionally
        // it does get used by itself, and the trailing separatorChar causes no problems.
        basePath = MemoryBank.userDataHome + File.separatorChar;
    }

    NoteGroupFile() {
        super();
        saveIsOngoing = false;
        saveWithoutData = false;
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

    // Child classes can override, if needed.  Currenly only CalendarNoteGroups need to,
    // since their filenames have a timestamp that changes with every save.  That timestamping
    // is the foundation of being able to archive earlier files, but the feature did not ever
    // get fully developed.
    protected String getGroupFilename() {
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


    // To arrive at this point we have already ensured that either the data is entirely new or there has been
    // some change to it, so at least some of the processing here is known to be needed.
    // Steps:
    //   1.  Move the old file (if any) out of the way.
    //   2.  Bail out early if there is no reason to create a file for this data.
    //   3.  Get the full path and filename
    //   4.  Verify the path
    //   5.  Write the data to a new file
    @Override // The file-flavored implementation of the NoteGroupDataAccessor interface method
    public AccessResult saveNoteGroupData() {
        //AppUtil.localDebug(true);
        saveIsOngoing = true;
        File f;

        // Now here is an important consideration - in this class we have a member that holds the associated filename,
        // (groupFilename) but we could also just extract it from the inner data.  So is there a difference?
        // Possibly.  In some cases the data from a file that has been loaded will be saved into a file with a
        // different name.  This can happen when filenames contain timestamps (possily for archiving) and also in
        // support of a 'saveAs' operation.  In any case, we treat the separate groupFilename as the one that was
        // loaded, and as for the one to save to, we ask the implementing child class what name to use.


        // Step 1 - Move the old file (if any) out of the way.
        // If we have a value in groupFilename at this point then it should mean that a file for it has been
        // successfully loaded in the current session, and that is the one that should be removed.
        // In this case we can trust the 'legality' of the name and path; we just need to verify that the file exists
        // and if so, delete it so that it does not conflict when we save the updated info.
        if (!groupFilename.isEmpty()) {
            MemoryBank.debug("NoteGroupFile.saveNoteGroupData: old filename = " + groupFilename);
            if (MemoryBank.archive) { // Archive the file
                MemoryBank.debug("  Archiving: " + shortName());
                // Note: need to fully implement archiving but for now, what happens
                // is what does not happen - we simply do not delete the old version.
            } else { // Need to delete the file
                f = new File(groupFilename);
                if(f.exists()) { // It must exist, for the delete to succeed.  If it already doesn't exist - we can live with that.
                    // Deleting (or archiving, if ever implemented) as the first step is necessary in case the
                    // current change is to just delete the information; we might not have any data to save.
                    if (!deleteFile(f)) { // If we continued after this then the save would fail; may as well stop now.
                        failureReason = "Failed to delete " + groupFilename;
                        System.out.println("Error - " + failureReason);
                        saveIsOngoing = false;
                        return AccessResult.FAILURE;
                    }
                }
            } // end if archive or delete
        } // end if

        // Step 2 - Bail out early if there is no reason to create a file for this data.
        if(isEmpty() && !saveWithoutData) {
            saveIsOngoing = false;
            return AccessResult.UNNECESSARY;
        }

        // Step 3 - Get the full path and filename
        // Here we let the child NoteGroup tell us the name of the file to save to; it may be different
        // than the one we aready have (and theoretically not the same as the one in the data either).
        setGroupFilename(getGroupFilename()); // use it directly but set it with data from a (possibly overridden) getter.
        MemoryBank.debug("  Saving NoteGroup data in " + shortName());

        // Step 4 - Verify the path
        f = new File(groupFilename);
        if (f.exists()) { // If the file already exists -
            // Having now established the filename to save to, we need to check for pre-existence.
            // If the filename is different that the one we started with then the new one might conceivably
            // already exist, but we could also have arrived here in the case where the filename had NOT
            // changed, although that is much less likely (such as a rogue thread that recreated the file
            // after we had deleted it, above).
            if (f.isDirectory()) { // If somehow the file is actually a directory -
                failureReason = groupFilename + " is a pre-existing directory";
            } else { // Not a directory, but still shouldn't be here.
                // At this point we might want to do/try another deletion, IF the name is different than it was for the
                // deletion earlier.     // but that's just a thought; don't bother if not going to be needed.
                failureReason = groupFilename + " is a pre-existing file";
            }
            System.out.println("Error - " + failureReason);
            saveIsOngoing = false;
            return AccessResult.FAILURE;
        } else { // The file does not already exist; create the path to it, if needed.
            String strThePath;
            strThePath = groupFilename.substring(0, groupFilename.lastIndexOf(File.separatorChar));
            f = new File(strThePath);
            if (!f.exists()) { // The directory path does not exist
                if (!f.mkdirs()) { // Create the directory path down to the level you need.
                    failureReason = "Unable to create this directory path: " + strThePath;
                    System.out.println("Error - " + failureReason);
                    saveIsOngoing = false;
                    return AccessResult.FAILURE;
                } // end if directory creation failed
            } else { // The directory path does exist; make sure that it IS a directory.
                if (!f.isDirectory()) {
                    failureReason = strThePath + " is not a directory!";
                    System.out.println("Error - " + failureReason);
                    saveIsOngoing = false;
                    return AccessResult.FAILURE;
                } // end if not a directory
            } // end if/else the path exists
        } // end if/else the file exists

        // Step 5 - Write the data to a new file
        Object[] theGroup = getTheData();
        writeDataToFile(groupFilename, theGroup);

        //AppUtil.localDebug(false);
        saveIsOngoing = false;
        return AccessResult.SUCCESS;
    } // end saveNoteGroupData


    // All child classes of NoteGroupFile have direct access to groupFilename, and as such
    // do not need to go through this method in order to change it.  However, I send them
    // all through here so that we never have to wonder where/how it got changed.  Now if
    // it goes off the rails, a simple debug session with a breakpoint here will show us
    // where the problem is.  This has happened enough times before now that there is no
    // question as to whether or not it is needed.  It definitely is needed.
    // And we take the opportunity to 'trim' it, while we're at it.
    void setGroupFilename(String newName) {
        String newGroupName;
        if(newName == null) newGroupName = "";
        else newGroupName = newName.trim();
        groupFilename = newGroupName;
    }

    GroupProperties getGroupProperties() {
        return myProperties;
    } // end getGroupProperties


    // Returns the filename-only component of the group file name.
    // Used in debug printouts.
    private String shortName() {
        String s = getGroupFilename();
        int ix = s.lastIndexOf(File.separatorChar);
        if (ix != -1) s = s.substring(ix + 1);
        return s;
    } // end shortName


    // Write the Group data to a file.  Provide the full path and filename, including extension.
    private void writeDataToFile(String theFilename, Object[] theGroup) {
        BufferedWriter bw = null;
        Exception e = null;
        try {
            FileOutputStream fileStream = FileUtils.openOutputStream(new File(theFilename)); // Creates parent directories, if needed.
            OutputStreamWriter writer = new OutputStreamWriter(fileStream, StandardCharsets.UTF_8);
            bw = new BufferedWriter(writer);
            bw.write(AppUtil.toJsonString(theGroup));
            // Set the number of notes written, only AFTER the write.
        } catch (Exception ex) {
            // This is a catch-all for other problems that may arise, such as finding a subdirectory of the
            // same name in the directory where you want to put the file, or not having write permission.
            e = ex;
        } finally {
            if (e != null) {
                // This one may have been ignorable; print the message and see.
                System.out.println("Exception in NoteGroupFile.writeDataToFile: \n  " + e.getMessage());
                failureReason = e.getMessage();
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
    }



}
