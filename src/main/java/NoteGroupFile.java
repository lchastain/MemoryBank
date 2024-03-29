import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

// Name is short but should be: NoteGroupFileDataAccessor.
@SuppressWarnings("rawtypes")
class NoteGroupFile extends FileDataAccessor implements NoteGroupDataAccessor {
    boolean saveWithoutData;  // This can allow for empty search results, brand new TodoLists, etc.
    boolean saveIsOngoing; // A 'state' flag used by getGroupFilename (for now; other uses are possible).
    GroupInfo myGroupInfo;

    private String failureReason; // Various file access failure reasons, or null.
    private ChronoUnit dateType;
    private CalendarNoteGroup.CalendarFileFilter fileFilter;

    static StringBuilder ems = new StringBuilder();  // Error Message String
    static int MAX_FILENAME_LENGTH = 20; // Somewhat arbitrary, but helps with UI issues.

    // This is the FULL filename, with storage specifier & path, prefix and extension.
    // Access it with getGroupFilename() & setGroupFilename().
    private String groupFilename;

    NoteGroupFile(GroupInfo groupInfo) {
        myGroupInfo = groupInfo;

        // No need to have a filename hanging around until we actually use it.
        // First real need is to capture it once a file is loaded.
        setGroupFilename(null); // This sets it to an empty string (not really null).

        saveIsOngoing = false;
        failureReason = null;

        // Default
        saveWithoutData = false;

        // Calendar-type groups do not need to be saved when they have no data.
        // Some of the others - saving them creates a placeholder for a new group where notes will
        //   be added subsequently, and/or they have extended properties that should be
        //   preserved even though there are no notes to go along with them, such as the
        //   search panel settings of a SearchResultGroup.
        switch (groupInfo.groupType) {
            case DAY_NOTES -> dateType = ChronoUnit.DAYS;
            case MONTH_NOTES -> dateType = ChronoUnit.MONTHS;
            case YEAR_NOTES -> dateType = ChronoUnit.YEARS;
            case SEARCH_RESULTS -> saveWithoutData = true;
            case TODO_LIST -> saveWithoutData = true;
            case LOG -> saveWithoutData = false;
            case EVENTS -> saveWithoutData = true;
            case GOALS -> saveWithoutData = true;
            case MILESTONE -> saveWithoutData = false;
        }
        if (dateType != null) fileFilter = new CalendarNoteGroup.CalendarFileFilter(dateType);
    }

    //-------------------------------------------------------------------
    // Method Name:  checkFilename
    //
    // Check theProposedName for the following 'illegal' file naming conditions:
    //   No entry, or whitespace only.
    //   The name ends in '.json'.
    //   The name contains more than MAX_FILENAME_LENGTH chars.
    //   The filesystem refuses to create a file with this name.
    //
    // Return Value - A 'complaint' string if name is not valid,
    //    otherwise an empty string.
    //
    // static rather than default due to external references, but all of
    // those could easily be changed, except for TodoNoteGroup.saveAs;
    // and moving it to here breaks too many other method calls from
    // within that one to local methods in the TodoNoteGroup instance.
    // Of course there could be solutions to that too, but how far down
    // that road do we want to go, for what added value?  Don't know
    // about the value so how far is 'not one step' (for now); this works.
    // But now - what about moving this one to FileGroup?  think about it.
    //-------------------------------------------------------------------
    static String checkFilename(String theProposedName, String basePath) {
        ems.setLength(0);

        String testName = theProposedName.trim();

        // Check for non-entry (or evaporation).
        if (testName.isEmpty()) {
            ems.append("No New List Name was supplied!");
        }

        // Refuse unwanted help.
        if (testName.endsWith(".json")) {
            ems.append("The name you supply cannot end in '.json'");
        } // end if

        // Check for legal max length.
        // The input field used in this class would not allow this one, but
        //   since this method is static and accepts input from outside
        //   sources, this check should be done.
        if (testName.length() > MAX_FILENAME_LENGTH) {
            ems.append("The new name is limited to ").append(MAX_FILENAME_LENGTH).append(" characters");
        } // end if

        // If there have been any problems found up to this point then we don't want to
        // continue checking, so return the answer now.
        if (!ems.toString().isEmpty()) return ems.toString();

        // Do we want to check to see if a file with this name already exists?
        //   No.  The handler for the renaming event will disallow that particular
        //   situation, so we don't need to repeat that logic here.  Likewise for
        //   the 'Save As' logic.
        //
        //   As for the 'Add New...' functionality that is also a consumer of this
        //   method, the AppTreePanel handles that situation by simply opening it, a
        //   kind of back-door selection of the existing list.

        // Note - I thought it would be a good idea to check for 'illegal'
        //   characters in the filename, but when I started testing, the
        //   Windows OS accepted %, -, $, ! and &; not sure what IS illegal.
        //   Of course there ARE illegal characters, and the ones above may
        //   still be illegal on another OS.  So, the best way to
        //   detect them is to try to create a file using the name we're
        //   checking.  Any io error and we can fail this check.

        // Now try to create the file, with an additional '.test' extension.  This will not
        // conflict with any 'legal' existing file in the directory, so if there is any
        // problem at all then we know it's a bad name.
        String theFilename = basePath + testName + ".test";
        File f = new File(theFilename);
        MemoryBank.debug("Name checking new file: " + f.getAbsolutePath());
        // This existence is not the same check as the one that we said would not be done; different extension.

        // Now ensure that the container directory is already there.
        boolean b;
        b = f.mkdirs();

        if (b) b = f.exists();
        try { // If the File operations below generate any exceptions, it's a bad name.
            // This first part & check are just to ensure that we do the rest of the test with
            // a 'clean' slate and don't have 'leftovers' from some earlier attempt(s).
            if (b) {
                b = f.delete(); // This MIGHT throw an exception.
                if (!b && f.exists()) {
                    // It shouldn't have existed in the first place, and now it
                    // refuses to be deleted.  This is enough cause for concern
                    // to justify disallowing the new name.  No Exception was
                    // thrown in this case but we will complain about it anyway -
                    ems.append("A previous test file refuses to be deleted.");
                }
            } else {
                b = f.createNewFile();
                if (!b || !f.exists()) {
                    // This probably would not happen since any name that does
                    // not cause an Exception should result in a created file.
                    // But it's less risky to just check for and handle it anyway.
                    ems.append("Unable to create a test file with the proposed name");
                } else {
                    // Ok, so we just now created the test file, but did it really get the requested name?
                    // This is not such an outlandish question - a filename with a colon in it CAN be created
                    // but the Windows OS will just drop the colon and anything after it.  The createNewFile()
                    // method appears to be much more permissive than the renameTo(), so we will now test the
                    // rename as well, and we don't even have to try a different name.
                    b = f.renameTo(new File(theFilename));
                    if (!b) { // No Exception thrown, but no joy with the rename operation, either.
                        ems.append("The proposed new name was rejected by the operating system!");
                    }

                    // Ok, so it was renamed to itself.  Now delete it, and verify that this was done as well.
                    b = f.delete();
                    if (!b || f.exists()) {
                        ems.append("Unable to remove the test file with the proposed new name!");
                    }
                }
            }
        } catch (IOException | SecurityException e) {
            ems.append(e.getMessage());
        } // end try/catch

        return ems.toString();
    } // end checkFilename


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


    // Can be called directly, or as the first step of the 'save' operation.
    public void deleteNoteGroupData() {
        if (!groupFilename.isEmpty()) {
            MemoryBank.debug("NoteGroupFile.saveNoteGroupData: old filename = " + groupFilename);
            File f = new File(groupFilename);
            if (f.exists()) { // It must exist, for the delete to succeed.  If it already doesn't exist - we can live with that.
                // Deleting (or archiving, if ever implemented) as the first step of saving is necessary in case the
                // current change is to just delete the information; we might not have any data to save.
                if (!deleteFile(f)) { // If we continued after this then the save would fail; may as well stop now.
                    failureReason = "Failed to delete " + groupFilename;
                    System.out.println("Error - " + failureReason);
                    saveIsOngoing = false;
                }
            }
        } // end if
    } // end deleteNoteGroupData


    public boolean exists() {
        String theFilename = foundFilename();
        return !theFilename.isEmpty();
    }

    String foundFilename() {
        return foundFilename(myGroupInfo);
    }

    // Given a GroupInfo, this method will return the full name and path (if it exists) of the file
    // where the data for the group is persisted.  If no file exists, the return string is empty ("").
    String foundFilename(GroupInfo groupInfo) {
        String theFilename = "";
        String foundName = "";
        String areaPath;
        String filePrefix;
        LocalDate theChoice;

        // Possible additional prefixing, for Archives.
        String firstPart = basePath;
        if (groupInfo.archiveName != null) {
            String fixedName = groupInfo.getArchiveStorageName();
            firstPart += DataArea.ARCHIVES.getAreaName() + File.separatorChar;
            firstPart += fixedName + File.separatorChar;
        }

        switch (groupInfo.groupType) {
            case GOALS -> {
                areaPath = firstPart + DataArea.GOALS.getAreaName() + File.separatorChar;
                filePrefix = goalGroupFilePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
            }
            case MILESTONE -> {
                areaPath = firstPart + DataArea.GOALS.getAreaName() + File.separatorChar;
                filePrefix = milestoneGroupFilePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
            }
            case GOAL_LOG -> {
                areaPath = firstPart + DataArea.GOALS.getAreaName() + File.separatorChar;
                filePrefix = logFilePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
            }
            case NOTES -> {
                areaPath = firstPart + DataArea.NOTES.getAreaName() + File.separatorChar;
                filePrefix = noteFilePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
            }
            case GOAL_NOTES -> {
                areaPath = firstPart + DataArea.GOALS.getAreaName() + File.separatorChar;
                filePrefix = noteFilePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
            }
            case GOAL_TODO -> {
                areaPath = firstPart + DataArea.GOALS.getAreaName() + File.separatorChar;
                filePrefix = todoListFilePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
            }
            case EVENTS -> {
                areaPath = firstPart + DataArea.UPCOMING_EVENTS.getAreaName() + File.separatorChar;
                filePrefix = eventGroupFilePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
            }
            case DAY_NOTES -> {
                theChoice = CalendarNoteGroup.getDateFromGroupName(groupInfo);
                theFilename = foundFilename(theChoice, "D");
            }
            case MONTH_NOTES -> { // Example group name:  October 2020
                theChoice = CalendarNoteGroup.getDateFromGroupName(groupInfo);
                theFilename = foundFilename(theChoice, "M");
            }
            case YEAR_NOTES -> { // Example group name:  2020
                theChoice = CalendarNoteGroup.getDateFromGroupName(groupInfo);
                theFilename = foundFilename(theChoice, "Y");
            }
            case LOG -> {
                areaPath = firstPart + DataArea.LOGS.getAreaName() + File.separatorChar;
                filePrefix = logFilePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
            }
            case TODO_LIST -> {
                areaPath = firstPart + DataArea.TODO_LISTS.getAreaName() + File.separatorChar;
                filePrefix = todoListFilePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
            }
            case SEARCH_RESULTS -> {
                areaPath = firstPart + DataArea.SEARCH_RESULTS.getAreaName() + File.separatorChar;
                filePrefix = searchResultFilePrefix;
                theFilename = areaPath + filePrefix + groupInfo.getGroupName() + ".json";
            }
        }

        if (!theFilename.isEmpty() && new File(theFilename).exists()) {
            foundName = theFilename;
        }

        return foundName;
    }


    // Given a LocalDate and a type of note to look for ("D", "M", or "Y", this method
    // will return the appropriate filename if a file exists for the indicated Date/Type.
    // If no file exists, the return string is empty ("").
    static String foundFilename(LocalDate theDate, String dateType) {
        String[] foundFiles = null;
        String lookfor = dateType;
        String fileName = calendarNoteGroupAreaPath;
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
         assert foundFiles != null;
   System.out.println("foundFiles: " + foundFiles.length);
   for(String s: foundFiles) System.out.println(s);
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


    // This method will return a Date (if it can) that is constructed from the input File's filename and path.
    // It relies on the parent directory of the file being named for the corresponding year, and the name of
    // the file (prior to the underscore) indicating the date within the year.
    // If the parsing operations trip over a bad format, a null will be returned.
    static LocalDate getDateFromFilename(File f) {
        String strAbsolutePath = f.getAbsolutePath();
        String strTheName = f.getName();
        MemoryBank.debug("Looking for date in filename: " + strAbsolutePath);

        // Initial format checking -
        if (!strTheName.contains("_"))
            return null;
        boolean badName = !strTheName.startsWith("D");
        if (strTheName.startsWith("M"))
            badName = false;
        if (strTheName.startsWith("Y"))
            badName = false;
        if (badName)
            return null;

        // Parse the Year from the path -
        int theYear;
        try {
            int index2 = strAbsolutePath.indexOf(strTheName) - 1;
            int index1 = strAbsolutePath.substring(0, index2).lastIndexOf(
                    File.separatorChar) + 1;

            String strTheYear = strAbsolutePath.substring(index1, index2);
            try {
                theYear = Integer.parseInt(strTheYear);
            } catch (NumberFormatException nfe) {
                return null;
            } // end try/catch
        } catch (IndexOutOfBoundsException ioobe) {
            return null;
        }

        // Get the Month from the filename, if present.
        int theMonth = 1;
        if (!strTheName.startsWith("Y")) {
            // Then it starts with either 'D' or 'M', which means that
            // it has a 'Month' component that must be converted.
            try { // Position of the 2-digit Month in the filename is fixed.
                int index1 = 1;
                int index2 = 3;

                String strTheMonth = strTheName.substring(index1, index2);
                try {
                    theMonth = Integer.parseInt(strTheMonth);
                } catch (NumberFormatException nfe) {
                    return null;
                } // end try to parse the integer
            } catch (IndexOutOfBoundsException ioobe) {
                return null;
            } // end try to cut the substring
        } // end if

        int theDay = 1;
        if (strTheName.startsWith("D")) {
            try { // Position of the 2-digit Day in the filename is fixed.
                int index1 = 3;
                int index2 = 5;

                String strTheDay = strTheName.substring(index1, index2);
                try {
                    theDay = Integer.parseInt(strTheDay);
                } catch (NumberFormatException nfe) {
                    return null;
                } // end try to parse the integer
            } catch (IndexOutOfBoundsException ioobe) {
                return null;
            } // end try to cut the substring
        } // end if

        LocalDate dateFromFilename = LocalDate.of(theYear, theMonth, theDay);
        MemoryBank.debug("Made date from filename: " + dateFromFilename);
        return dateFromFilename;
    } // end getDateFromFilename


    // This method searches the repository for the next data file in the indicated direction.
    // If it finds one, it returns the associated date.  If there are no more data files in
    // that direction then it simply returns the next date in that direction.
    @Override
    public LocalDate getNextDateWithData(LocalDate initialDate, ChronoUnit dateDelta, CalendarNoteGroup.Direction direction) {
        LocalDate returnDate;
        int initialYear = initialDate.getYear();

        // Get a list of all Years (directories) where the user has data
        // Potential issue here if we're not looking into a directory (but we know that we are).
        // And if the directory is empty?  Not too much of a problem; it will still 'sort', and then we don't iterate.
        File theYears = new File(calendarNoteGroupAreaPath); // All directories here, with 4-digit numerical names.
        List<File> yearsList = Arrays.asList(Objects.requireNonNull(theYears.listFiles()));

        // Sort the Years according to the direction we will be searching.
        // But also - set a default return value in case nothing is found.
        if (direction == CalendarNoteGroup.Direction.FORWARD) {
            Collections.sort(yearsList);
            returnDate = initialDate.plus(1, dateDelta);
        } else {  // direction == BACKWARD
            yearsList.sort(Collections.reverseOrder());
            returnDate = initialDate.minus(1, dateDelta);
        }

        // Cycle through the sorted directories -
        String theName;
        for (File f : yearsList) {
            theName = f.getName();
            if(theName.startsWith(".")) continue; // Mac may add a hidden file here.
            int dataYear = Integer.parseInt(theName);

            // Skip all Years that are in the 'wrong' direction from the initialDate
            if (direction == CalendarNoteGroup.Direction.FORWARD) {
                if (dataYear < initialYear) continue;
            } else {
                if (dataYear > initialYear) continue;
            }
            if (dateType == ChronoUnit.YEARS && dataYear == initialYear) continue;

            // Now we can start looking in this Year for data -
            System.out.println("Looking in: " + f.getAbsolutePath());

            LocalDate theDate = getDataDate(f, returnDate, direction);
            if (theDate != null) { // This will be the one we were looking for.
                returnDate = theDate;
                break;
            }
            // Otherwise, we keep looking as long as there are 'Year' directories remaining to be searched -
        }

        return returnDate; // This is just the default return value.
    }

    // Get the date of the next data file (that has data) in the direction we are looking.
    private LocalDate getDataDate(File yearDirectory, LocalDate initialDate, CalendarNoteGroup.Direction direction) {
        // Get a list of the right type of data files that are in the yearDirectory -
        File[] dataFiles = yearDirectory.listFiles(fileFilter);
        if (dataFiles == null || dataFiles.length == 0) return null; // The null check covers possible I/O errors.

        List<File> filesList = Arrays.asList(dataFiles);

        // Sort the data files according to the direction we will be searching.
        if (direction == CalendarNoteGroup.Direction.FORWARD) {
            Collections.sort(filesList);
        } else {  // direction == BACKWARD
            filesList.sort(Collections.reverseOrder());
        }

        for (File f : filesList) {
            LocalDate aDate = getDateFromFilename(f);
            assert aDate != null;
            //System.out.println("The date for " + f.getAbsolutePath() + " is " + aDate.toString());

            if (direction == CalendarNoteGroup.Direction.FORWARD) {
                if (aDate.isBefore(initialDate)) continue;
            } else {
                if (aDate.isAfter(initialDate)) continue;
            }

            // Look into the file to see if it has significant content.
            Object[] theGroup = NoteGroupFile.loadFileData(f);
            boolean itHasData = false;
            if (theGroup.length == 1) {
                // It is not possible to have a length of one for DayNoteData, where the content is GroupProperties.
                // When only linkages are present in DayNoteData, the file still contains a two-element array
                // although the second element may be null.  So this is just older (pre linkages) data, and in
                // that case it is significant since we didn't ever save 'empty\ days.
                itHasData = true;  // and DayNoteData did not get saved, if no notes.
            } else { // new structure; element zero is a GroupProperties.  But nothing from the Properties is
                // significant for purposes of this method; only the length of the second element (the ArrayList).
                ArrayList arrayList = AppUtil.mapper.convertValue(theGroup[1], ArrayList.class);
                if (arrayList.size() > 0) itHasData = true;
            }
            if (itHasData) return aDate;

        }
        return null;
    }


    // Returns a String containing the requested portion of the input LocalDateTime.
    // Years are expected to be 4 digits long, all other units are two digits.
    // For hours, the full range (0-23) is returned; no adjustment to a 12-hour clock.
    private static String getTimePartString(LocalDateTime localDateTime, ChronoUnit cu, Character padding) {

        switch (cu) {
            case YEARS -> {
                StringBuilder theYears = new StringBuilder(String.valueOf(localDateTime.getYear()));
                if (padding != null) {
                    while (theYears.length() < 4) {
                        theYears.insert(0, padding);
                    }
                }
                return theYears.toString();
            }
            case MONTHS -> {
                String theMonths = String.valueOf(localDateTime.getMonthValue());
                if (padding != null) {
                    if (theMonths.length() < 2) theMonths = padding + theMonths;
                }
                return theMonths;
            }
            case DAYS -> {
                String theDays = String.valueOf(localDateTime.getDayOfMonth());
                if (padding != null) {
                    if (theDays.length() < 2) theDays = padding + theDays;
                }
                return theDays;
            }
            case HOURS -> {
                String theHours = String.valueOf(localDateTime.getHour());
                if (padding != null) {
                    if (theHours.length() < 2) theHours = padding + theHours;
                }
                return theHours;
            }
            case MINUTES -> {
                String theMinutes = String.valueOf(localDateTime.getMinute());
                if (padding != null) {
                    if (theMinutes.length() < 2) theMinutes = padding + theMinutes;
                }
                return theMinutes;
            }
            case SECONDS -> {
                String theSeconds = String.valueOf(localDateTime.getSecond());
                if (padding != null) {
                    if (theSeconds.length() < 2) theSeconds = padding + theSeconds;
                }
                return theSeconds;
            }
            default -> throw new IllegalStateException("Unexpected value: " + cu);
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
    public Object[] loadNoteGroupData() {
        return loadNoteGroupData(myGroupInfo);
    }


    @Override // The NoteGroupDataAccessor method implementation.
    public Object[] loadNoteGroupData(GroupInfo groupInfo) {
        // Get the Filename for the GroupInfo.  Refresh it
        //   just prior to loading the group rather than earlier, because the Panel content may
        //   have changed so that the file to load now is not the same as it was at group construction;
        //   for CalendarNoteGroups the filename for the group depends on the base date shown in the panel.
        String theFilename = foundFilename(groupInfo);
        // If no such file was found then the 'theFilename' string will be empty ("").

        // We need to retain the filename for this group, even if no file was found.
        // This will be needed during the save operation, to let us know if a pre-existing
        // file should be removed before we save.
        if(myGroupInfo == groupInfo) {
            // The condition is needed for when this accessor is used to load some 'other' group of the same type,
            //   for example during a Merge operation; we don't want our own filename to change to that one.
            setGroupFilename(theFilename);
        }

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
        //   or an ArrayList (although I never called them that; I called them Vector).
        return loadFileData(theFilename);
    } // end loadNoteGroupData

    String makeFullFilename() {
        LocalDate theDate = null;
        String dateType = null;

        switch (myGroupInfo.groupType) {
            case DAY_NOTES:
                theDate = CalendarNoteGroup.getDateFromGroupName(myGroupInfo);
                dateType = "D";
                break;
            case MONTH_NOTES:  // areaName = "Years";
                theDate = CalendarNoteGroup.getDateFromGroupName(myGroupInfo);
                dateType = "M";
                break;
            case YEAR_NOTES:  // areaName = "Years";
                theDate = CalendarNoteGroup.getDateFromGroupName(myGroupInfo);
                dateType = "Y";
                break;
            case GOALS:
            case GOAL_LOG:
            case GOAL_TODO:
            case MILESTONE:
            case EVENTS:
            case LOG:
            case NOTES:
            case GOAL_NOTES:
            case TODO_LIST:
            case SEARCH_RESULTS:
                break;
            default:
                return null; // The other group types do not have associated File data.
        }

        if(theDate != null) {
            return makeFullFilename(theDate, dateType);
        } else {
            return makeFullFilename(myGroupInfo.groupType, myGroupInfo.getGroupName());
        }
    }


    // This method develops a variable filename that depends on the requested
    // noteType (one of Year, Month, or Date, specified by Y, M, or D).
    // Examples:  Y_timestamp, M03_timestamp, D0704_timestamp.
    // The numeric Year for these files is known by a parent directory.
    // Used in saving of Calendar-based data files.
    static String makeFullFilename(LocalDate localDate, String dateType) {
        StringBuilder filename = new StringBuilder(calendarNoteGroupAreaPath);
        filename.append(localDate.getYear());
        filename.append(File.separatorChar);
        filename.append(dateType);

        if (!dateType.equals("Y")) {
            filename.append(getTimePartString(localDate.atTime(0, 0), ChronoUnit.MONTHS, '0'));

            if (!dateType.equals("M")) {
                filename.append(getTimePartString(localDate.atTime(0, 0), ChronoUnit.DAYS, '0'));
            } // end if not a Month note
        } // end if not a Year note

        filename.append("_").append(getTimestamp()).append(".json");
        return filename.toString();
    }

    String makeFullFilename(GroupType groupType, String groupName) {
        String areaName = "NoArea";    // If these turn up in the data, it's a problem.
        String prefix = "NoPrefix";    // But at least we'll know where to look.

        switch (groupType) {
            case GOALS -> {
                areaName = DataArea.GOALS.getAreaName();
                prefix = goalGroupFilePrefix;
            }
            case GOAL_LOG -> {
                areaName = DataArea.GOALS.getAreaName();
                prefix = logFilePrefix;
            }
            case GOAL_TODO -> {
                areaName = DataArea.GOALS.getAreaName();
                prefix = todoListFilePrefix;
            }
            case MILESTONE -> {
                areaName = DataArea.GOALS.getAreaName();
                prefix = milestoneGroupFilePrefix;
            }
            case EVENTS -> {
                areaName = DataArea.UPCOMING_EVENTS.getAreaName();
                prefix = eventGroupFilePrefix;
            }
            case LOG -> {
                areaName = DataArea.LOGS.getAreaName();
                prefix = logFilePrefix;
            }
            case NOTES -> {
                areaName = DataArea.NOTES.getAreaName();
                prefix = noteFilePrefix;
            }
            case GOAL_NOTES -> {
                areaName = DataArea.GOALS.getAreaName();
                prefix = noteFilePrefix;
            }
            case TODO_LIST -> {
                areaName = DataArea.TODO_LISTS.getAreaName();
                prefix = todoListFilePrefix;
            }
            case SEARCH_RESULTS -> {
                areaName = DataArea.SEARCH_RESULTS.getAreaName();
                prefix = searchResultFilePrefix;
            }
            default -> {
            }
            // The other types do not have associated File data.
        }
        return basePath + areaName + File.separatorChar + prefix + groupName + ".json";
    }


    // We don't actually have to open the file in order to
    // determine its GroupInfo; the answers can be gleaned solely from the path and name.  So we 'get' the
    // info from the object that is a File class, using methods other than opening and reading the file.  The
    // filesystem does not even need to contain such a file in order for this method to succeed.
    static GroupInfo getGroupInfoFromFilePath(File theFile) {
        GroupInfo theAnsr = new GroupInfo();
        String theFullFilename = theFile.toString();
        if (StringUtils.containsIgnoreCase(theFullFilename, File.separatorChar + DataArea.CALENDARS.getAreaName() + File.separatorChar)) {
            String nameOnly = theFile.getName();
            GroupType groupType = GroupType.NOTES;

            // Ok, the use of magic numbers here is flaky; I will look for a better way.
            if (nameOnly.length() == 25) groupType = GroupType.DAY_NOTES;
            if (nameOnly.length() == 23) groupType = GroupType.MONTH_NOTES;
            if (nameOnly.length() == 21) groupType = GroupType.YEAR_NOTES;
            theAnsr.groupType = groupType;

            LocalDate theDate = getDateFromFilename(theFile);
            String theName = CalendarNoteGroup.getGroupNameForDate(theDate, groupType);
            theAnsr.setGroupName(theName);
        } else if (StringUtils.containsIgnoreCase(theFullFilename, File.separatorChar + DataArea.TODO_LISTS.getAreaName() + File.separatorChar)) {
            theAnsr.groupType = GroupType.TODO_LIST;
            theAnsr.setGroupName(getGroupNameFromFilename(theFullFilename));
        } else if (StringUtils.containsIgnoreCase(theFullFilename, File.separatorChar + DataArea.NOTES.getAreaName() + File.separatorChar)) {
            theAnsr.groupType = GroupType.NOTES;
            theAnsr.setGroupName(getGroupNameFromFilename(theFullFilename));
        } else if (StringUtils.containsIgnoreCase(theFullFilename, File.separatorChar + DataArea.SEARCH_RESULTS.getAreaName() + File.separatorChar)) {
            theAnsr.groupType = GroupType.SEARCH_RESULTS;
            theAnsr.setGroupName(getGroupNameFromFilename(theFullFilename));
        } else if (StringUtils.containsIgnoreCase(theFullFilename, File.separatorChar + DataArea.GOALS.getAreaName() + File.separatorChar)) {
            theAnsr.groupType = GroupType.GOALS;
            theAnsr.setGroupName(getGroupNameFromFilename(theFullFilename));
        } else if (StringUtils.containsIgnoreCase(theFullFilename, File.separatorChar + DataArea.UPCOMING_EVENTS.getAreaName() + File.separatorChar)) {
            theAnsr.groupType = GroupType.EVENTS;
            theAnsr.setGroupName(getGroupNameFromFilename(theFullFilename));
        } else {
            // Return will be a GroupInfo with null for name & type.
            System.out.println("NoteGroupFile.getGroupInfoFromFilePath: unrecognized filename: " + theFullFilename);
        }

        return theAnsr;
    }


    // A formatter for a String that is a filename specifier.  It strips
    //   away the File path, separators, prefix and ending, leaving only
    //   the base (pretty) name of the file.  It works left-to-right, as
    //   the input shrinks (in case that's important for you to know).
    // Usage is intended for data files of non-Calendar NoteGroups;
    //   Calendar NoteGroups use a different 'name' and do not need
    //   to prettify the path+name of their data file since it is not
    //   intended to be shown to users.
    static String getGroupNameFromFilename(String theLongName) {
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
    } // end getGroupNameFromFilename


    @Override
    public String getObjectionToName(String theName) {
        // It is important to check filename validity in the area where the new file would be created,
        // so that any possible Security Exception is seen.  Those Exceptions may not be seen in a
        // different area of the same filesystem.
        File aFile = new File(makeFullFilename(myGroupInfo.groupType, theName));
        return checkFilename(theName, aFile.getParent() + File.separatorChar);
    }

    @Override
    public boolean renameNoteGroupData(GroupType groupType, String nodeName, String renamedTo) {
        String oldNamedFile = makeFullFilename(groupType, nodeName);
        String newNamedFile = makeFullFilename(groupType, renamedTo);
        MemoryBank.debug("Full name of original file: " + oldNamedFile);
        File f = new File(oldNamedFile);
        return f.renameTo(new File(newNamedFile));
    }

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

        // Now here is an important consideration - in this class we have a member that holds the associated filename,
        // (groupFilename) but we could also just extract it from the inner data.  So is there a difference?
        // Possibly.  In some cases the data from a file that has been loaded will be saved into a file with a
        // different name.  This can happen when filenames are constructed from timestamps (possily for archiving) and
        // also in support of a 'saveAs' operation.  In any case, we treat the separate groupFilename as the one that
        // was loaded, and as for the one to save to, we ask the implementing child class what name to use, and don't
        // actually get into the data to see what file it came from and thinks it should go back to.

        // Step 1 - Move the old file (if any) out of the way.
        // If we have a value in groupFilename at this point then it should mean that a file for it has been
        // successfully loaded in the current session, and that is the one that should be removed.
        // In this case we can trust the 'legality' of the name and path; we just need to verify that the file exists
        // and if so, delete it so that it does not conflict when we save a new file with the updated info.
        deleteNoteGroupData();
        if (!saveIsOngoing) return; // A problem in deleting might have derailed the save operation.

        // Step 2 - Bail out early if there is no reason to create a file for this data.
        if (theData == null && !saveWithoutData) {
            saveIsOngoing = false;
            return;
        }

        // Step 3 - Get the full path and filename
        // Here we use the GroupInfo we were constructed with to determine the name of the file to save to; it may be
        // different than the one we aready have (and theoretically not a match to the GroupInfo in the data either).
        setGroupFilename(getGroupFilename()); // Set it according to the getter.
        MemoryBank.debug("  Saving NoteGroup data in " + groupFilename);

        // Step 4 - Verify the path
        File f = new File(groupFilename);
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
    @Override
    public String getFailureReason() {
        return failureReason;
    }


    // This method is used to obtain a filename for data that needs to be saved.  We cannot accept "" for an answer.
    private String getGroupFilename() {
        String s = myGroupInfo.groupType.toString();
        if (s.endsWith("Note")) {  // This will cover the Calendar Note type groups.
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
        if (!foundName.isEmpty()) {
            return foundName;
        } else {
            return makeFullFilename();
        }
    }


    // Intercept a null setting to convert it to empty string.
    // And otherwise we take the opportunity to 'trim' it.
    void setGroupFilename(String newName) {
        String newGroupName;
        if (newName == null) newGroupName = "";
        else newGroupName = newName.trim();
        groupFilename = newGroupName;
    }


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

} // end NoteGroupFile class
