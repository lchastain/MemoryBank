import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Vector;


class NoteGroupFile extends NoteGroup implements NoteGroupDataAccessor {
    static String basePath;
    boolean saveWithoutData;  // This can allow for empty search results, brand new TodoLists, etc.
    boolean saveIsOngoing;

    private String failureReason; // Various file access failure reasons, or null.

    // This is the FULL filename, with storage specifier & path, prefix and extension.
    // Access it with getGroupFilename() & setGroupFilename().
    protected String groupFilename;


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
        failureReason = null;
        saveWithoutData = false;
    }


    NoteGroupFile(GroupProperties groupProperties) {
        this();
        setGroupProperties(groupProperties);
    }


    @Override
    // This is the file-flavored implementation of the NoteGroupDataAccessor interface method
    @SuppressWarnings("rawtypes")
    public boolean addDayNote(LocalDate theDay, DayNoteData theNote) {
        Object[] theGroup = null; // The complete data set for the group to which we will add theNote.
        NoteGroupFile noteGroupFile;

        // Get the group name for the input date.
        String dayGroupName = NoteGroupDataAccessor.getGroupNameForDay(theDay);

        // Now get the DayNoteGroupPanel from the application tree
        NoteGroupPanel dayNoteGroupPanel;
        dayNoteGroupPanel = AppTreePanel.theInstance.theAppDays;

        if(dayNoteGroupPanel != null) { // Ok it's not null -
            // so now we have to ask - does it currently display the group that we want to add to?
            // It might be 'pointing' to some other day.
            String theDaysName = dayNoteGroupPanel.getGroupName();
            if (theDaysName.equals(dayGroupName)) { //
                // If it does point to the same day then it might have unsaved changes.
                if(dayNoteGroupPanel.groupChanged) {
                    // If there are unsaved changes then we save the file now,
                    // so that we can add to the closed file, in the next steps.
                    dayNoteGroupPanel.preClosePanel();
                }
                AppTreePanel.theInstance.theAppDays = null;
                // This was so that it gets reloaded if re-selected later in the tree.
                // Doing it this way was better than how reverse links handle their display update, because a
                // refresh now still would not handle the new note that we are about to add to the data file.
            }
        }

        // Determine the filename.  We cannot just go directly to making one, because pre-existing files will
        //   have a previously set filename that was built with timestamps earlier than any we would make here.
        //   So look for pre-existing and take that one if it exists, otherwise make one.
        String theFilename = NoteGroupFile.foundFilename(theDay, "D");
        if (theFilename.equals("")) {
            theFilename = NoteGroupFile.makeFullFilename(theDay, "D");
        } else {
            // Now we have to try to load the data directly from file.
            theGroup = loadFileData(theFilename);
        } // end if

        // Make a new NoteGroupFile -
        if(theGroup != null) { // Data was loaded from a file -
            // Convert theGroup[0] to a GroupProperties
            GroupProperties groupProperties = AppUtil.mapper.convertValue(theGroup[0], GroupProperties.class);

            // Convert theGroup[1] to a Vector of NoteData, and add the new note to it.
            TypeReference theType = new TypeReference<Vector<DayNoteData>>() { };
            Vector<NoteData> noteDataVector = AppUtil.mapper.convertValue(theGroup[1], theType);
            noteDataVector.add(theNote);

            // Make a new NoteGroupFile
            noteGroupFile = new NoteGroupFile(groupProperties);
            noteGroupFile.setGroupFilename(theFilename);
            noteGroupFile.add(noteDataVector);
        } else { // Othwerwise, we were not able to load the data (there might have been an error but more likely
            // there was simply no pre-existing data for the Day).  So we just make our own data and NoteGroupFile -
            GroupProperties groupProperties = new GroupProperties(dayGroupName, GroupInfo.GroupType.DAY_NOTES);
            Vector<NoteData> noteDataVector = new Vector<>(1,1);
            noteDataVector.add(theNote);

            noteGroupFile = new NoteGroupFile(groupProperties);
            noteGroupFile.setGroupFilename(theFilename);
            noteGroupFile.add(noteDataVector);
        }

        // Note that this is an instance method but we aren't saving its own data; instead saving the data for a new
        // NoteGroupFile that was created by this method.  So this method could have been static but that would
        // disqualify this method from being defined in the interface because the interface needs it to be non-static
        // since each implementor will have a different methodology and data store in which to add a note.
        //    yes, it could have been done differently .... better.   The calling contexts could have instantiated
        //    the NoteGroupFile that already held the data that they needed and then called an addNote method on it
        //    from there.  Might redo this...
        noteGroupFile.saveNoteGroupData();
        return true;
    } // end addDayNote

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
                // Only a 'day' CalendarNoteGroup group name will parse properly.
                // The other two - need some pre-processing.
                theChoice = LocalDate.parse(groupInfo.getGroupName());
                theFilename = foundFilename(theChoice, "D");
                break;
            case MONTH_NOTES: // Example group name:  October 2020
                theChoice = LocalDate.parse("15 " + groupInfo.getGroupName());
                theFilename = foundFilename(theChoice, "M");
                break;
            case YEAR_NOTES: // Example group name:  2020
                theChoice = LocalDate.now();
                int theYear = Integer.parseInt(groupInfo.getGroupName());
                theFilename = foundFilename(theChoice.withYear(theYear), "Y");
                break;
            case TODO_LIST:
                areaPath = TodoNoteGroupPanel.areaPath;
                filePrefix = TodoNoteGroupPanel.filePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
                break;
            case SEARCH_RESULTS:
                areaPath = SearchResultGroupPanel.areaPath;
                filePrefix = SearchResultGroupPanel.filePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
                break;
            case EVENTS:
                areaPath = EventNoteGroupPanel.areaPath;
                filePrefix = EventNoteGroupPanel.filePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
                break;
            case GOALS:
                areaPath = GoalGroupPanel.areaPath;
                filePrefix = GoalGroupPanel.filePrefix;
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
        String fileName = CalendarNoteGroupPanel.areaPath;
        fileName += String.valueOf(theDate.getYear());

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
        } // end if exists

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
            fileName = CalendarNoteGroupPanel.areaPath;
            fileName += String.valueOf(theDate.getYear()); // There may be a problem here if we look at other-than-four-digit years
            fileName += File.separatorChar;
            fileName += foundFiles[foundFiles.length - 1];
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
    // NOT BEING CALLED ...  ??  and that leads to an unused 'foundFilename(GroupInfo)' -
    public void loadNoteGroupData(GroupInfo groupInfo) {
        // Get the Filename for the GroupInfo.
        String theFilename = foundFilename(groupInfo);
        setGroupFilename(theFilename); // Yes, even if empty.  We don't want to leave an old one in place.
        if(theFilename.isEmpty()) return; // No filename == no data.

        Object[] theData = loadFileData(theFilename);
        setTheData(theData);
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
        StringBuilder filename = new StringBuilder(CalendarNoteGroupPanel.areaPath);
        filename.append(getTimePartString(localDate.atTime(0, 0), ChronoUnit.YEARS, '0'));
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


    static String makeFullFilename(String areaName, String groupName) {
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
        return basePath + areaName + File.separatorChar + prefix + groupName + ".json";
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
    @Override // This is the file-flavored implementation of the NoteGroupDataAccessor interface method
    public AccessResult saveNoteGroupData() {
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


    // Provides a way for a calling context to retrieve the most recent result.
    // If there was not a failure then this value will be null.
    String getFailureReason() {
        return failureReason;
    }


    // Child classes can override, if needed.  Currenly only CalendarNoteGroups need to,
    // since their filenames have a timestamp that changes with every save.  That timestamping
    // is the foundation of being able to archive earlier files, but the feature did not ever
    // get fully developed.
    protected String getGroupFilename() {
        return groupFilename;
    }


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
