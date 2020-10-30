import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

class NoteGroupFile implements NoteGroupDataAccessor {
    static String basePath;
    static String calendarNoteGroupAreaPath;
    static String eventGroupAreaPath;
    static String goalGroupAreaPath;
    static String searchResultGroupAreaPath;
    static String todoListGroupAreaPath;

    static String eventGroupFilePrefix;
    static String goalGroupFilePrefix;
    static String searchResultFilePrefix;
    static String todoListFilePrefix;

    boolean saveWithoutData;  // This can allow for empty search results, brand new TodoLists, etc.
    boolean saveIsOngoing; // A 'state' flag used by getGroupFilename (for now; other uses are possible).
    GroupInfo groupInfo;

    private String failureReason; // Various file access failure reasons, or null.

    // This is the FULL filename, with storage specifier & path, prefix and extension.
    // Access it with getGroupFilename() & setGroupFilename().
    private String groupFilename;


    static {
        basePath = MemoryBank.userDataHome + File.separatorChar;

        calendarNoteGroupAreaPath = basePath + NoteGroup.calendarNoteGroupArea + File.separatorChar;
        eventGroupAreaPath = basePath + NoteGroup.eventGroupArea + File.separatorChar;
        goalGroupAreaPath = basePath + NoteGroup.goalGroupArea + File.separatorChar;
        searchResultGroupAreaPath = basePath + NoteGroup.searchResultGroupArea + File.separatorChar;
        todoListGroupAreaPath = basePath + NoteGroup.todoListGroupArea + File.separatorChar;

        eventGroupFilePrefix = "event_";
        goalGroupFilePrefix = "goal_";
        searchResultFilePrefix = "search_";
        todoListFilePrefix = "todo_";
    }

//    NoteGroupFile() {
//        super();
//        saveIsOngoing = false;
//        failureReason = null;
//        saveWithoutData = false;
//    }


    NoteGroupFile(GroupInfo groupInfo) {
        this.groupInfo = groupInfo;

// No need to have a filename hanging around until we actually use it.
// First real need is to capture it once a file is loaded.
        // Try to find an existing file first.  If found, use that filename.
        // Otherwise - make one.
//        String theFilename = foundFilename(groupInfo);
//        if(theFilename.isEmpty()) {
//            theFilename = makeFullFilename();
//        }
//        setGroupFilename(theFilename);
        setGroupFilename(null); // This sets it to an empty string.

        saveIsOngoing = false;
        failureReason = null;

        // Only the Calendar-type groups do not need to be saved when they have no data.
        // The others - saving them creates a placeholder for a new group where notes will
        //   be added subsequently, and/or they have extended properties that should be
        //   preserved even though there are no notes to go along with them, such as the
        //   search panel settings of a SearchResultGroup.
        saveWithoutData = groupInfo.groupType == GroupInfo.GroupType.TODO_LIST; // Initialize the flag.
        if(groupInfo.groupType == GroupInfo.GroupType.GOALS) saveWithoutData = true;
        if(groupInfo.groupType == GroupInfo.GroupType.SEARCH_RESULTS) saveWithoutData = true;
        if(groupInfo.groupType == GroupInfo.GroupType.EVENTS) saveWithoutData = true;
    }


    protected boolean deleteFile(File f) {
        // There are a couple of cases where we could try to delete a file that is not there
        // in the first place.  So - we verify that the file is really there and only if it is
        // do we try to delete it.  Then we check for existence again because the deletion can
        // occasionally return a false negative (because of gc/timing issues?).
        // But File.exists() does not ever return a false positive (that I've seen).
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

    String foundFilename() {
        return foundFilename(groupInfo);
    }

        // Given a GroupInfo, this method will return the full name and path (if it exists) of the file
    // where the data for the group is persisted.  If no file exists, the return string is empty ("").
    static String foundFilename(GroupInfo groupInfo) {
        String theFilename = "";
        String foundName = "";
        String areaPath;
        String filePrefix;
        LocalDate theChoice;
        switch (groupInfo.groupType) {
            case DAY_NOTES:
                theChoice = CalendarNoteGroup.getDateFromGroupName(groupInfo);
                theFilename = foundFilename(theChoice, "D");
                break;
            case MONTH_NOTES: // Example group name:  October 2020
                theChoice =  CalendarNoteGroup.getDateFromGroupName(groupInfo);
                theFilename = foundFilename(theChoice, "M");
                break;
            case YEAR_NOTES: // Example group name:  2020
                theChoice =  CalendarNoteGroup.getDateFromGroupName(groupInfo);
                theFilename = foundFilename(theChoice, "Y");
                break;
            case TODO_LIST:
                areaPath = todoListGroupAreaPath;
                filePrefix = todoListFilePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
                break;
            case SEARCH_RESULTS:
                areaPath = searchResultGroupAreaPath;
                filePrefix = searchResultFilePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
                break;
            case EVENTS:
                areaPath = eventGroupAreaPath;
                filePrefix = eventGroupFilePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
                break;
            case GOALS:
                areaPath = goalGroupAreaPath;
                filePrefix = goalGroupFilePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
                break;
        }
        if(!theFilename.isEmpty() && new File(theFilename).exists()) {
            foundName = theFilename;
        }

        return foundName;
    }


    // Given a LocalDate and a type of note to look for ("D", "M", or "Y", this method
    // will return the appropriate filename if a file exists for the indicated timeframe.
    // If no file exists, the return string is empty ("").
    static String foundFilename(LocalDate theDate, String dateType) {
        String[] foundFiles = null;
        String lookfor = dateType;
        String fileName = calendarNoteGroupAreaPath;
        fileName += String.valueOf(theDate.getYear());
//        StringBuilder fileName = new StringBuilder(CalendarNoteGroupPanel.areaPath); // May want to use a StringBuilder here, instead.
//        fileName.append(String.valueOf(theDate.getYear()));


        // System.out.println("Looking in " + fileName);
        File f = new File(fileName);
        if (f.exists()) {  // If the Year of the Date is an existing directory -
            if (f.isDirectory()) {

                if (!dateType.equals("Y")) { // ..and if we're not looking for a YearNote -
                    lookfor += getTimePartString(theDate.atTime(0, 0), ChronoUnit.MONTHS, '0');

                    if (!dateType.equals("M")) {  // ..and if we are looking for a DayNote -
                        lookfor += getTimePartString(theDate.atTime(0, 0), ChronoUnit.DAYS, '0');
                    } // end if not a Month note
                } // end if not a Year note
                lookfor += "_";

                // System.out.println("Looking for " + lookfor);
                foundFiles = f.list(new AppUtil.logFileFilter(lookfor));
            } // end if directory
        } // end if the directory for the year exists

        // Reset this local variable, and reuse.
        fileName = "";

        // A 'null' foundFiles only happens if directory is not there;
        // a valid condition that needs no further action.  Similarly,
        // the directory might exist but be empty; also allowed.
        if ((foundFiles != null) && (foundFiles.length > 0)) {
            // Previously we tried to handle the case of more than one file found for the same
            // name prefix, but the JOptionPane error dialog cannot be shown here because if
            // this occurs at startup then we'd never get past the splash screen.  So - we just
            // take the last one.  But having a pile-up of older files, if it happens, could
            // become a big problem.  So far this HAS happened but on a one or two file basis,
            // never hundreds, and it was due to glitches during development, where a debug
            // session was killed.  So - taking the last one will suffice for now, until the
            // app is converted to storing its data in a database vs the filesystem and then
            // the problem goes away.  Currently the timestamp portion of the filename does not
            // assist in group identification anyway; it is a placeholder for archiving, and that
            // feature is still a long long way from realization (this note 10 Oct 2020, idea to
            // do archiving - came about sometime in 1998, I believe).
            fileName = calendarNoteGroupAreaPath;
            fileName += String.valueOf(theDate.getYear()); // There may be a problem here if we look at other-than-four-digit years
            fileName += File.separatorChar;
            fileName += foundFiles[foundFiles.length - 1]; // Without archiving there should only be one.  Take the 'last' one.
        }
        return fileName;
    } // end findFilename


    // Returns a String containing the requested portion of the input LocalDateTime.
    // Years are expected to be 4 digits long, all other units are two digits.
    // For hours, the full range (0-23) is returned; no adjustment to a 12-hour clock.
    private static String getTimePartString(LocalDateTime localDateTime, ChronoUnit cu, Character padding) {

        switch (cu) {
            case YEARS:
                StringBuilder theYears = new StringBuilder(String.valueOf(localDateTime.getYear()));
                if (padding != null) {
                    while (theYears.length() < 4) {
                        theYears.insert(0, padding);
                    }
                }
                return theYears.toString();
            case MONTHS:
                String theMonths = String.valueOf(localDateTime.getMonthValue());
                if (padding != null) {
                    if (theMonths.length() < 2) theMonths = padding + theMonths;
                }
                return theMonths;
            case DAYS:
                String theDays = String.valueOf(localDateTime.getDayOfMonth());
                if (padding != null) {
                    if (theDays.length() < 2) theDays = padding + theDays;
                }
                return theDays;
            case HOURS:
                String theHours = String.valueOf(localDateTime.getHour());
                if (padding != null) {
                    if (theHours.length() < 2) theHours = padding + theHours;
                }
                return theHours;
            case MINUTES:
                String theMinutes = String.valueOf(localDateTime.getMinute());
                if (padding != null) {
                    if (theMinutes.length() < 2) theMinutes = padding + theMinutes;
                }
                return theMinutes;
            case SECONDS:
                String theSeconds = String.valueOf(localDateTime.getSecond());
                if (padding != null) {
                    if (theSeconds.length() < 2) theSeconds = padding + theSeconds;
                }
                return theSeconds;
            default:
                throw new IllegalStateException("Unexpected value: " + cu);
        }
    } // end getTimePartString


    // Returns a string of numbers representing a Date
    //   and time in the format:  yyyyMMddHHmmSS
    // Used in unique filename creation.
    static String getTimestamp() {
        StringBuilder theStamp;

        LocalDateTime ldt = LocalDateTime.now();

        theStamp = new StringBuilder(getTimePartString(ldt, ChronoUnit.YEARS, null));
        theStamp.append(getTimePartString(ldt, ChronoUnit.MONTHS, '0'));
        theStamp.append(getTimePartString(ldt, ChronoUnit.DAYS, '0'));
        theStamp.append(getTimePartString(ldt, ChronoUnit.HOURS, '0'));
        theStamp.append(getTimePartString(ldt, ChronoUnit.MINUTES, '0'));
        theStamp.append(getTimePartString(ldt, ChronoUnit.SECONDS, '0'));

        return theStamp.toString();
    } // end getTimestamp

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


    @Override // The NoteGroupDataAccessor method implementation.
    public Object[] loadNoteGroupData(GroupInfo groupInfo) {
   // This comment belongs in a calling context, where the referenced content is changed.
        // Get the Filename for the GroupInfo.  Refresh it
        //   just prior to loading the group rather than earlier, because the Panel content may
        //   have changed so that the file to load now is not the same as it was at group construction;
        //   for CalendarNoteGroups the filename for the group depends on the base date shown in the panel.
        String theFilename = foundFilename(groupInfo);
        // If no such file was found then the 'theFilename' string will be empty ("").

        // We need to retain the filename for this group, even if no file was found.
        // This will be needed during the save operation, to let us know if a pre-existing
        // file should be removed before we save.
        setGroupFilename(theFilename);

        if (theFilename.isEmpty()) {
            MemoryBank.debug("No file was found for: " + groupInfo.getGroupName());
            // If we didn't find a file then we can return a null right now.  Of course we could have skipped
            // this line and just used the 'file not found' logic of the load method, but this saves the extra
            // processing to handle the expected load failure as well as avoiding two unneeded method calls.
            return null; // No filename == no data.
        }

        MemoryBank.debug("File for " + groupInfo.getGroupName() + " is: " + theFilename);
        //   From the file, the raw data comes in as a JSON string that can be parsed as an
        //   array of one or two Objects that identify themselves as either a LinkedHashMap
        //   or an ArrayList (although I never said that they should be).
        return loadFileData(theFilename);
    }

    // -----------------------------------------------------------------
    // Method Name: makeFullFilename
    //
    // This method develops a variable filename that depends on the requested
    // noteType (one of Year, Month, or Date, specified by Y, M, or D).
    // Examples:  Y_timestamp, M03_timestamp, D0704_timestamp.
    // The numeric Year for these files is known by a parent directory.
    // Used in saving of Calendar-based data files.
    // It is kept here (for now?) as opposed to the CalendarNoteGroup
    // because of the additional calls to two static methods also here.
    // BUT - there is no reason that those two could not also move
    // over there, since this method (and findFilename) is their only 'client'.
    // -----------------------------------------------------------------
    static String makeFullFilename(LocalDate localDate, String noteType) {
        StringBuilder filename = new StringBuilder(calendarNoteGroupAreaPath);
//        filename.append(getTimePartString(localDate.atTime(0, 0), ChronoUnit.YEARS, '0'));
        filename.append(localDate.getYear());
        filename.append(File.separatorChar);
        filename.append(noteType);

        if (!noteType.equals("Y")) {
            filename.append(getTimePartString(localDate.atTime(0, 0), ChronoUnit.MONTHS, '0'));

            if (!noteType.equals("M")) {
                filename.append(getTimePartString(localDate.atTime(0, 0), ChronoUnit.DAYS, '0'));
            } // end if not a Month note
        } // end if not a Year note

        filename.append("_").append(getTimestamp()).append(".json");
        return filename.toString();
    }

    // This should work but it could be cleaner, without the reachout for the CNG types.
    String makeFullFilename() {
        String areaName = "NoArea";    // If these turn up in the data, it's a problem.
        String prefix = "NoPrefix";    // But at least we'll know where to look.
        String groupName = "NoGroup";
        LocalDate theDate;

        switch(groupInfo.groupType) {
            case DAY_NOTES:
                //areaName = "Years";
                theDate = CalendarNoteGroup.getDateFromGroupName(groupInfo);
                return makeFullFilename(theDate, "D");
            case MONTH_NOTES:
                //areaName = "Years";
                theDate = CalendarNoteGroup.getDateFromGroupName(groupInfo);
                return makeFullFilename(theDate, "M");
            case YEAR_NOTES:
                //areaName = "Years";
                theDate = CalendarNoteGroup.getDateFromGroupName(groupInfo);
                return makeFullFilename(theDate, "Y");
            case GOALS:
                areaName = "Goals";
                prefix = goalGroupFilePrefix;
                groupName = groupInfo.getGroupName();
                break;
            case EVENTS:
                areaName = "UpcomingEvents";
                prefix = eventGroupFilePrefix;
                groupName = groupInfo.getGroupName();
                break;
            case TODO_LIST:
                areaName = "TodoLists";
                prefix = todoListFilePrefix;
                groupName = groupInfo.getGroupName();
                break;
            case SEARCH_RESULTS:
                areaName = "SearchResults";
                prefix = searchResultFilePrefix;
                groupName = groupInfo.getGroupName();
                break;
            default:
                // The other types do not have associated File data.
        }
        return basePath + areaName + File.separatorChar + prefix + groupName + ".json";
    }


    static String makeFullFilename(String areaName, String groupName) {
        String prefix = "";
        switch (areaName) {
            case "Goals":
                prefix = goalGroupFilePrefix;
                break;
            case "UpcomingEvents":
                prefix = eventGroupFilePrefix;
                break;
            case "TodoLists":
                prefix = todoListFilePrefix;
                break;
            case "SearchResults":
                prefix = searchResultFilePrefix;
                break;
        }
        return basePath + areaName + File.separatorChar + prefix + groupName + ".json";
    }

    // A convenience method so that an instance can make a call to the static method
    //   without having to provide the parameter.
    String prettyName() {
        return prettyName(groupFilename);
    } // end prettyName


    // A formatter for a String that is a filename specifier.  It strips
    //   away the File path, separators, prefix and ending, leaving only
    //   the base (pretty) name of the file.  It works left-to-right, as
    //   the input shrinks (in case that's important for you to know).
    // Usage is intended for data files of non-Calendar NoteGroups;
    //   Calendar NoteGroups use a different 'name' and do not need
    //   to prettify the path+name of their data file since it is not
    //   intended to be shown to users.
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
    @Override // This is the file-flavored implementation of the NoteGroupDataAccessor interface method
    public void saveNoteGroupData(Object[] theData) {
        //AppUtil.localDebug(true);
        saveIsOngoing = true;
        failureReason = null;
        File f;

        // Now here is an important consideration - in this class we have a member that holds the associated filename,
        // (groupFilename) but we could also just extract it from the inner data.  So is there a difference?
        // Possibly.  In some cases the data from a file that has been loaded will be saved into a file with a
        // different name.  This can happen when filenames are constructed from timestamps (possily for archiving) and
        // also in support of a 'saveAs' operation.  In any case, we treat the separate groupFilename as the one that
        // was loaded, and as for the one to save to, we ask the implementing child class what name to use, and don't
        // actually get into the data to see what file it thinks it should go to.  This seems wrong somehow...

        // Step 1 - Move the old file (if any) out of the way.
        // If we have a value in groupFilename at this point then it should mean that a file for it has been
        // successfully loaded in the current session, and that is the one that should be removed.
        // In this case we can trust the 'legality' of the name and path; we just need to verify that the file exists
        // and if so, delete it so that it does not conflict when we save a new file with the updated info.
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
                        return;
                    }
                }
            } // end if archive or delete
        } // end if

        // Step 2 - Bail out early if there is no reason to create a file for this data.
        if(theData == null && !saveWithoutData) {
            saveIsOngoing = false;
            return;
        }

        // Step 3 - Get the full path and filename
        // Here we use the GroupInfo we were constructed with to determine the name of the file to save to; it may be
        // different than the one we aready have (and theoretically not a match to the GroupInfo in the data either).
        setGroupFilename(getGroupFilename()); // Set it according to the getter.
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
            return;
        } else { // The file does not already exist; create the path to it, if needed.
            String strThePath;
            strThePath = groupFilename.substring(0, groupFilename.lastIndexOf(File.separatorChar));
            f = new File(strThePath);
            if (!f.exists()) { // The directory path does not exist
                if (!f.mkdirs()) { // Create the directory path down to the level you need.
                    failureReason = "Unable to create this directory path: " + strThePath;
                    System.out.println("Error - " + failureReason);
                    saveIsOngoing = false;
                    return;
                } // end if directory creation failed
            } else { // The directory path does exist; make sure that it IS a directory.
                if (!f.isDirectory()) {
                    failureReason = strThePath + " is not a directory!";
                    System.out.println("Error - " + failureReason);
                    saveIsOngoing = false;
                    return;
                } // end if not a directory
            } // end if/else the path exists
        } // end if/else the file exists

        // Step 5 - Write the data to a new file
        writeDataToFile(groupFilename, theData);

        //AppUtil.localDebug(false);
        saveIsOngoing = false;
    } // end saveNoteGroupData


    // Provides a way for a calling context to retrieve the most recent result.
    // If there was not a failure then this value will be null.
    String getFailureReason() {
        return failureReason;
    }


    // This method is used to obtain a filename for data that needs to be saved.  We cannot accept "" for an answer.
    private String getGroupFilename() {
        String s = groupInfo.groupType.toString();
        if(s.endsWith("Note")) {  // This will cover the Calendar Note type groups.
            // Their filenames ends in a timestamp that changes with every save.  That timestamping
            // is the foundation of being able to archive earlier files, but the feature did not ever
            // get fully developed.  This handling variant for it remains here for now.
            if (saveIsOngoing) {  // For right now (25 Oct 2020), it always will be since that is the only usage.
                // In this case we need a new filename; need to make one because (due to timestamping)
                // it should not already exist.
                return makeFullFilename();
            } // end if save ongoing
        } // end if CalendarNoteGroup

        String foundName = foundFilename(); // Results of a search may be "".
        if(!foundName.isEmpty()) {
            return foundName;
        } else {
            return makeFullFilename();
        }
    }


    // Intercept a null setting to convert it to empty string.
    // And otherwise we take the opportunity to 'trim' it.
    void setGroupFilename(String newName) {
        String newGroupName;
        if(newName == null) newGroupName = "";
        else newGroupName = newName.trim();
        groupFilename = newGroupName;
    }


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
            } // end if there was an exception
            try {
                if (bw != null) {
                    // These flush/close lines may seem like overkill, but there is internet support for being so cautious.
                    bw.flush();
                    bw.close(); // Also closes the wrapped FileWriter
                }
            } catch (Exception ex) { // This one would be more serious - raise a 'louder' alarm.
                // Most likely would be an IOException, but we catch them all, to be sure.
                failureReason = ex.getMessage();
                ex.printStackTrace(System.out);
            } // end try/catch
        } // end try/catch
    }



}
