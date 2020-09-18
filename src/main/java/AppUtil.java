import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.WordUtils;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.FilenameFilter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

// This class provides static utility methods that are needed by more than one client-class in the main app.
// It makes more sense to collect them into one utility class, than to try to decide which user class
// should house a given method while the other user classes then have to somehow get access to it.
public class AppUtil {
    static ObjectMapper mapper;

    private static Boolean blnGlobalArchive;
    private static Boolean blnGlobalDebug; // initially null

    static {
        mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    } // end static


    // Put this in a 'view' base class....
    static boolean[][] findDataDays(int year) {
        boolean[][] hasDataArray = new boolean[12][31];
        // Will need to normalize the month integers from 0-11 to 1-12
        // and the day integers from 0-30 to 1-31

        // System.out.println("Searching for data in the year: " + year);
//        String FileName = CalendarNoteGroup.basePath() + year;
        String FileName = CalendarNoteGroupPanel.areaPath + year;
        MemoryBank.debug("Looking in " + FileName);

        String[] foundFiles = null;

        File f = new File(FileName);
        if (f.exists()) {
            if (f.isDirectory()) {
                foundFiles = f.list(new logFileFilter("D"));
            } // end if directory
        } // end if exists

        if (foundFiles == null)
            return hasDataArray;
        int month, day;

        // System.out.println("Found:");
        for (String foundFile : foundFiles) {
            month = Integer.parseInt(foundFile.substring(1, 3));
            day = Integer.parseInt(foundFile.substring(3, 5));
            // System.out.println(" " + foundFiles[i]);
            // System.out.println("\tMonth: " + month + "\tDay: " + day);
            hasDataArray[month - 1][day - 1] = true;
        } // end for i

        return hasDataArray;
    } // end findDataDays

    // -----------------------------------------------------------------
    // Method Name: findFilename
    //
    // Given a LocalDate and a type of note to look for ("D", "M", or "Y", this method
    // will return the appropriate filename if a file exists for the indicated timeframe.
    // If no file exists, the return string is empty ("").
    // -----------------------------------------------------------------
    static String findFilename(LocalDate theDate, String dateType) {
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
                foundFiles = f.list(new logFileFilter(lookfor));
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
            // the problem goes away.
            // Also - you don't have to use a timestamp in the filename, bozo.  The individual
            // data elements do each have their LMDs, and each 'prefix' is unique to the
            // containing 'year' directory, so what is the value-added, anyway?
            // Think about it...
            fileName = CalendarNoteGroupPanel.areaPath;
            fileName += String.valueOf(theDate.getYear()); // There may be a problem here if we look at other-than-four-digit years
            fileName += File.separatorChar;
            fileName += foundFiles[foundFiles.length - 1];
        }
        return fileName;
    } // end findFilename

    // -----------------------------------------------------------
    // Method Name: getDateFromFilename
    //
    // This method will return a Date (if it can) that is
    // constructed from the input File's filename and path.
    // It relies on the parent directory of the file being
    // named for the corresponding year, and the name of
    // the file (prior to the underscore) indicating the
    // date within the year.
    // If the parsing operations trip over a bad format,
    // a null will be returned.
    // -----------------------------------------------------------
    static LocalDate getDateFromFilename(File f) {
        String strAbsolutePath = f.getAbsolutePath();
        String strTheName = f.getName();
        MemoryBank.debug("Looking for date in filename: " + strAbsolutePath);

        // Initial format checking -
        if (!strTheName.contains("_"))
            return null;
        boolean badName = true;
        if (strTheName.startsWith("D"))
            badName = false;
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

    // -------------------------------------------------------------
    // Method Name: getTooltipString
    //
    // This method takes a string and returns it with inserted
    // line breaks, if needed.  Character column and line limits
    // are hardcoded.
    // -------------------------------------------------------------
    static String getTooltipString(String s) {
        StringBuilder theTooltipString = new StringBuilder();
        String[] strings = s.split("\n");
        int maxColumns = 60;
        int maxLines = 22;
        int lineCount = 0;
        // First, break on the line feeds that are already present in the string.
        // Then use the wrap utility to further break apart lines.
        // Keep a count of broken lines, observing the limit.
        for (String oneLine: strings) {
            if(lineCount > maxLines) break;
            String wrappedLine = WordUtils.wrap(oneLine, maxColumns, System.lineSeparator(), true);
            theTooltipString.append(wrappedLine);
            theTooltipString.append(System.lineSeparator()); // This is cr/lf in Windows, but we only show it vs storing it.
            lineCount++;
        }

        return theTooltipString.toString();
    }

    // A utility function to retrieve a specified JMenuItem.
    static JMenuItem getMenuItem(JMenu jMenu, String text) {
        JMenuItem theMenuItem = null;

        for (int j = 0; j < jMenu.getItemCount(); j++) {
            theMenuItem = jMenu.getItem(j);
            if (theMenuItem == null) continue; // Separator
            //System.out.println("    Menu Item text: " + jmi.getText());
            if (theMenuItem.getText().equals(text)) return theMenuItem;
        } // end for j

        return theMenuItem;
    }

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

    //--------------------------------------------------------
    // Method Name: getTimestamp
    //
    // Returns a string of numbers representing a Date
    //   and time in the format:  yyyyMMddHHmmSS
    // Used in unique filename creation.
    //--------------------------------------------------------
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

    // -------------------------------------------------------
    // Method Name: localArchive
    //
    // Call this method in pairs - first with a 'true'
    // param, then later with 'false'. Place the calls
    // as 'brackets' around code that would otherwise
    // cause a data file to be removed.
    // ok, this is kludgy but not sure if the 'fix' is
    // in the coding or just the comments. It works.
    // -------------------------------------------------------
    static void localArchive(boolean b) {
        if (b) {
            // Preserve the original setting
            blnGlobalArchive = MemoryBank.archive;

            // and turn on archiving
            MemoryBank.archive = true;
        } else {
            // Put the original setting back, if it had been changed.
            if (blnGlobalArchive != null)
                MemoryBank.archive = blnGlobalArchive;

            // and reset our local holding variable
            blnGlobalArchive = null;
        } // end if
    } // end localArchive

    // -----------------------------------------------------------------
    // Method Name: localDebug
    //
    // Logging statements throughout the code (MemoryBank.dbg and MemoryBank.debug)
    //   will only print out if the MemoryBank.debug boolean is true.  However,
    //   when this variable is true, the debugging printouts can be
    //   quite voluminous.  One solution is to set it to true for a
    //   specific section of code and then back to false, but that may
    //   alter the original value of the MemoryBank debug value.  If it had
    //   been false to start with then no worries but if it were true then
    //   by looking at just a section of code, we turn off all subsequent
    //   logging.  This method
    //   allows you to preserve the original MemoryBank.debug value while
    //   ensuring that it is on for a specific section of code.
    // Call this method in pairs - first with a 'true' parameter, then
    //   later with a 'false'.  Place the calls as 'brackets' around
    //   code that is problematic until the issues are resolved, then
    //   comment out or remove.  The individual logging statements
    //   within that code section can remain unchanged.
    //
    // AppUtil.localDebug(true);
    // AppUtil.localDebug(false);
    // -----------------------------------------------------------------
    public static void localDebug(boolean b) {
        String traceString;
        if (b) {
            // Preserve the original setting
            blnGlobalDebug = MemoryBank.debug;

            // and turn on debugging
            MemoryBank.debug = true;
            traceString = Thread.currentThread().getStackTrace()[2].toString();
            MemoryBank.debug("Turned on local debugging at: " + traceString);
        } else {
            traceString = Thread.currentThread().getStackTrace()[2].toString();
            MemoryBank.debug("Turned off local debugging at: " + traceString);

            // Put the original setting back, if it had been changed.
            if (blnGlobalDebug != null) MemoryBank.debug = blnGlobalDebug;

            // and reset our local holding variable
            blnGlobalDebug = null;
        } // end if
    } // end localDebug

    // -----------------------------------------------------------------
    // Method Name: makeFilename
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
    static String makeFilename(LocalDate localDate, String noteType) {
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

    // Wrap the text in html, used for JLabel text with style adjustments.
    static String makeHtml(String theString) {
        return "<html>" + theString + "</html>";
    }

    // This only works for JLabel text that is also wrapped in html
    static String makeRed(String theString) {
        String redOn = "<font color=#ff0000>";
        String redOff = "</font>";
        return redOn + theString + redOff;
    }



    static String makeTimeString(LocalTime localTime) {
        String theString;
        String timeOfDayString = localTime.toString();
        String hoursString = timeOfDayString.substring(0, 2);
        int theHours = Integer.parseInt(hoursString);
        String minutesString = timeOfDayString.substring(3, 5);

        if (DayNoteGroupPanel.dayNoteDefaults.military) {
            // drop out the colon and take just hours and minutes.
            theString = hoursString + minutesString;
        } else {  // Normalize to a 12-hour clock
            if (theHours > 12) {
                theString = (theHours - 12) + ":" + minutesString;
            } else {
                theString = theHours + ":" + minutesString;
            }
        }
        return theString;
    }


    // ----------------------------------------------------------------------
    // Inner class
    // ----------------------------------------------------------------------
    static class logFileFilter implements FilenameFilter {
        String which;

        logFileFilter(String s) {
            which = s;
        } // end constructor

        public boolean accept(File dir, String name) {
            boolean b1 = name.startsWith(which);
            boolean b2 = name.endsWith(".json");
            return b1 & b2;
        } // end accept
    } // end class logFileFilter


    static TreePath getTreePath(TreeNode treeNode) {
        TreeNode tn = treeNode;
        List<Object> nodes = new ArrayList<>();
        if (tn != null) {
            nodes.add(tn);
            tn = tn.getParent();
            while (tn != null) {
                nodes.add(0, tn);
                tn = tn.getParent();
            }
        }

        return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
    }

    static String toJsonString(Object theObject) {
        String theJson = "";
        try {
            theJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(theObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return theJson;
    }


    //---------------------------------------------------------------
    // Method Name: addNote
    //
    // Add a note to a DayNoteGroup.
    // Note that this is a static method, meaning that it can be called
    // from any other context without first instantiating a DayNoteGroup,
    // and that the file being added to may or may not already exist.
    // The call to saveData() is needed every time because even if
    // the calling context is an ongoing method processing several
    // notes, it will still call this method one note at a time and we don't
    // maintain a 'state' somewhere or buffer up the notes to be saved.
    //
    // Two known calling contexts add a note to a Day: TodoNoteComponent
    // (only does one at a time anyway) and EventNoteGroup (which works
    // from a list but each one in the list could go to a different day).
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean addNote(String theFilename, NoteData nd) {
        NoteGroupData noteGroupData = null;
        Object[] theGroup;

        // Note that a full file read and write IS needed; we cannot simply do an 'append' because
        // the new note needs to be inserted into an encapsulated ArrayList.
        // However, a database based methodology would not have that same restriction.
        // (note to self, for future upgrade).
        theGroup = NoteGroupFile.loadFileData(theFilename);
        // ^^^ HERE we needed instead to get any in-memory panel's data, and assign it to noteGroupData.

        // TODO SOMETHING RONG!!! HERE.  GroupProperties not being addressed.  Do not need to make a new NoteGroupData; it comes with NoteGroupFIle.

        // No pre-existing data file is ok in this case; we'll just make one.
        if (theGroup == null) {
            noteGroupData = new NoteGroupData();
//            ArrayList al = new ArrayList(); // reason for suppressing the 'rawtypes' warning.
            Vector<NoteData> noteDataVector = new Vector<>(1,1);
            noteGroupData.add(noteDataVector); // This is the data; Group Properties get made automatically, elsewhere.
            // TODO verify that ^^^ by moving todo data to a day that has not been viewed in the current session, AND has no pre-existing file.
        }

        NoteGroupFile noteGroupFile = new NoteGroupFile();
        noteGroupFile.setGroupFilename(theFilename);


        // Now here is the cool part - we don't actually need to get the loaded data into a Vector
        // of a specific type (even though we know that the elements are all DayNoteData); we can
        // just add the note to the array of LinkedHashMap.
        assert noteGroupData != null;
        theGroup = noteGroupData.getTheData();
        ((Vector) theGroup[theGroup.length-1]).add(nd); // reason for suppressing the 'unchecked' warning.
        noteGroupFile.add((GroupProperties) theGroup[0]);
        noteGroupFile.add((Vector) theGroup[1]);

//        int notesWritten = NoteGroupFile.saveGroupData(theFilename, theGroup);
        noteGroupFile.saveNoteGroupData();
//        return notesWritten >= 1;    TODO
        return true;
    } // end addNote

    // This is my own conversion, to numbers that matched these
    // that were being returned by Calendar queries, now deprecated.
    // I put this in place as a temporary remediation along the way
    // to updating the app to new Java 8 date/time classes.
    // TODO - refactor all usages of this method to use getDayOfWeek, remove this one.
    static int getDayOfWeekInt(LocalDate tmpDate) {
        switch (tmpDate.getDayOfWeek()) {
            case SUNDAY:
                return 1;
            case MONDAY:
                return 2;
            case TUESDAY:
                return 3;
            case WEDNESDAY:
                return 4;
            case THURSDAY:
                return 5;
            case FRIDAY:
                return 6;
            case SATURDAY:
                return 7;
        }
        return -1;
    }

} // end class AppUtil
