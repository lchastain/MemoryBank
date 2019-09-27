import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;

// This class provides static utility methods that are needed by more than one client-class in the main app.
// It makes more sense to collect them into one utility class, than to try to decide which user class
// should house a given method while the other user classes then have to somehow get access to it.
public class FixUtil {
    static GregorianCalendar calTemp;
    static ObjectMapper mapper = new ObjectMapper();

    private static Boolean blnGlobalArchive;
    private static Boolean blnGlobalDebug;

    static {
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setDateFormatSymbols(new DateFormatSymbols());

        calTemp = (GregorianCalendar) Calendar.getInstance();
        // Note: getInstance at this time returns a Calendar that
        // is actually a GregorianCalendar, but since the return
        // type is Calendar, it must be cast in order to assign.

        calTemp.setGregorianChange(new GregorianCalendar(1752, Calendar.SEPTEMBER,
                14).getTime());
    } // end static

    // Copies src file to dst file.
    // If the dst file does not exist, it is created
    public static void copy(File src, File dst) {
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } // end try/catch
    } // end copy


    // Put this in a 'view' base class....
    static boolean[][] findDataDays(int year) {
        boolean[][] hasDataArray = new boolean[12][31];
        // Will need to normalize the month integers from 0-11 to 1-12
        // and the day integers from 0-30 to 1-31

        // System.out.println("Searching for data in the year: " + year);
        String FileName = MemoryBank.userDataHome + File.separatorChar + year;
        // System.out.println("Looking in " + FileName);

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
    // Given a Calendar and a type of file to look for, this method
    // will return the appropriate filename if a file exists for the indicated timeframe.
    // If no file exists, the return string is empty ("").
    // -----------------------------------------------------------------
    static String findFilename(@NotNull GregorianCalendar cal, String which) {
        String[] foundFiles = null;
        String lookfor = which;
        String fileName = MemoryBank.userDataHome + File.separatorChar;
        fileName += String.valueOf(cal.get(Calendar.YEAR));

        // System.out.println("Looking in " + fileName);

        File f = new File(fileName);
        if (f.exists()) {
            if (f.isDirectory()) {

                if (!which.equals("Y")) {
                    lookfor += getMonthIntString(cal.get(Calendar.MONTH));

                    if (!which.equals("M")) {
                        int date = cal.get(Calendar.DATE);
                        if (date < 10)
                            lookfor += "0";
                        lookfor += String.valueOf(date);
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
        if((foundFiles != null) && (foundFiles.length > 0)) {
            // Previously we tried to handle the case of more than one file found for the same
            // name prefix, but the JOptionPane error dialog cannot be shown here (possibly
            // because it referenced a null parentComponent).  Further, if this occurs at
            // startup then we'd never get past the splash screen.  So - we just take the first one.
            fileName = MemoryBank.userDataHome + File.separatorChar;
            fileName += String.valueOf(cal.get(Calendar.YEAR));
            fileName += File.separatorChar;
            fileName += foundFiles[0];
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
    static Date getDateFromFilename(File f) {
        String strAbsolutePath = f.getAbsolutePath();
        String strTheName = f.getName();

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
        int theMonth = 0;
        if (!strTheName.startsWith("Y")) {
            // Then it starts with either 'D' or 'M', which means that
            // it has a 'Month' component that must be converted.
            try { // Position of the 2-digit Month in the filename is fixed.
                int index1 = 1;
                int index2 = 3;

                String strTheMonth = strTheName.substring(index1, index2);
                try {
                    theMonth = Integer.parseInt(strTheMonth) - 1;
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

        calTemp.set(Calendar.YEAR, theYear);
        calTemp.set(Calendar.MONTH, theMonth);
        calTemp.set(Calendar.DATE, theDay);
        calTemp.set(Calendar.HOUR_OF_DAY, 0);
        calTemp.set(Calendar.MINUTE, 0);
        calTemp.set(Calendar.SECOND, 0);

        // System.out.println("Get date from: " + strAbsolutePath);
        // System.out.println(" result: " + calTemp.getTime());
        return calTemp.getTime();
    } // end getDateFromFilename

    // -------------------------------------------------------------
    // Method Name: getBrokenString
    //
    // This method takes a string and returns it with inserted
    // line breaks, if needed, at or before the 'n'th position
    // and truncated to a maximum of 'l' lines. If 'l' is
    // zero, the result is not truncated.
    // -------------------------------------------------------------
    static String getBrokenString(String s, int n, int l) {
        StringBuilder strTheBrokenString;
        StringBuilder strNextLine;

        int intLastBreak; // Index into the strNextLine
        int intCurrentCharCount; // Number of chars this line
        int i; // index thru the input string

        // The calling context should NOT be sending these values -
        if (s == null)
            return null;
        if (l < 0)
            return null;
        // Therefore our response is immediate, uninformative, and unapologetic.

        int intLength = s.length();
        int intLineCount = 1;

        byte[] aryTheChars = s.getBytes();

        // Look through the character array and
        // insert line breaks as needed.
        strTheBrokenString = new StringBuilder();
        strNextLine = new StringBuilder();
        intLastBreak = -1;
        intCurrentCharCount = 0;
        for (i = 0; i < intLength; i++) {
            // Get the next char.
            intCurrentCharCount++;

            // Did we encounter a linefeed ?
            if (aryTheChars[i] == '\n') {
                // First, take the line up to (not including) the lf.
                strTheBrokenString.append(strNextLine);
                strNextLine = new StringBuilder();

                // Now, bail if we're at the line limit.
                if ((l > 0) && (intLineCount >= l))
                    break;

                // Otherwise, add the lf
                strTheBrokenString.append('\n');
                intLineCount++;

                // And reset the counts
                intCurrentCharCount = 0;
                intLastBreak = -1;
            } else {
                strNextLine.append((char) aryTheChars[i]);
            } // end if this char is a linefeed

            if (aryTheChars[i] == ' ') {
                // we encountered a space
                intLastBreak = intCurrentCharCount - 1;
            } else if (aryTheChars[i] == '\t') {
                // we encountered a tab
                intLastBreak = intCurrentCharCount - 1;
            } // end if

            // We let it go past so that if this next offending
            // character had been the linefeed, we'd have
            // done the reset, above. That we're here means
            // that this is not a linefeed char.
            if (intCurrentCharCount > n) {
                if (intLastBreak > 0) {
                    // We can break at the last whitespace
                    strTheBrokenString.append(strNextLine.substring(0, intLastBreak + 1));
                    strNextLine = new StringBuilder(strNextLine.substring(intLastBreak + 1));
                    intLastBreak = -1;
                    intCurrentCharCount = strNextLine.length();
                } else {
                    // The string has no logical breaks; just break here.
                    strTheBrokenString.append(strNextLine.substring(0, n + 1));
                    strNextLine = new StringBuilder(strNextLine.substring(n + 1)); // one char
                    intCurrentCharCount = strNextLine.length();
                } // end if we had a good place to break the line

                // Now, bail if we're at the line limit.
                if ((l > 0) && (intLineCount >= l)) {
                    strNextLine = new StringBuilder();
                    break;
                }

                // Otherwise, insert an lf and keep going
                intLineCount++;
                strTheBrokenString.append('\n');
            } // end if we need to break the line
        } // end for i

        // Take the remaining last partial line, if any.
        strTheBrokenString.append(strNextLine);

        // System.out.println("Final line count: " + intLineCount);

        return strTheBrokenString.toString();
    } // end getBrokenString

    //--------------------------------------------------------
    // Method Name: getMonthIntString
    //
    // Returns a two-character string representing the number
    //   of the input month integer in a range from 01-12.
    //--------------------------------------------------------
    private static String getMonthIntString(int month) {
        String s;

        // The switch is cumbersome but necessary since some values went
        // from 1-based to 0-based, between different Java versions.
        switch (month) {
            case Calendar.JANUARY:
                s = "01";
                break;
            case Calendar.FEBRUARY:
                s = "02";
                break;
            case Calendar.MARCH:
                s = "03";
                break;
            case Calendar.APRIL:
                s = "04";
                break;
            case Calendar.MAY:
                s = "05";
                break;
            case Calendar.JUNE:
                s = "06";
                break;
            case Calendar.JULY:
                s = "07";
                break;
            case Calendar.AUGUST:
                s = "08";
                break;
            case Calendar.SEPTEMBER:
                s = "09";
                break;
            case Calendar.OCTOBER:
                s = "10";
                break;
            case Calendar.NOVEMBER:
                s = "11";
                break;
            case Calendar.DECEMBER:
                s = "12";
                break;
            default:
                s = null;
        } // end switch
        return s;
    } // end getMonthIntString


    //--------------------------------------------------------
    // Method Name: getTimestamp
    //
    // Returns a string of numbers representing a Date
    //   and time in the format:  yyyyMMddHHmmSS
    // Used in unique filename creation.
    //--------------------------------------------------------
    static String getTimestamp() {
        StringBuilder s;

        calTemp.setTime(new Date());

        int year = calTemp.get(Calendar.YEAR);
        s = new StringBuilder(String.valueOf(year));

        // This should not be necessary unless the system time is hosed...
        while (s.length() < 4) {
            s.insert(0, "0");
        } // end while

        s.append(getMonthIntString(calTemp.get(Calendar.MONTH)));

        int date = calTemp.get(Calendar.DATE);
        if (date < 10)
            s.append("0");
        s.append(date);

        int hour = calTemp.get(Calendar.HOUR_OF_DAY);
        if (hour < 10)
            s.append("0");
        s.append(hour);

        int minute = calTemp.get(Calendar.MINUTE);
        if (minute < 10)
            s.append("0");
        s.append(minute);

        int second = calTemp.get(Calendar.SECOND);
        if (second < 10)
            s.append("0");
        s.append(second);

        return s.toString();
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
    //   specific section of code and then back to false, but then you
    //   may lose its original value in the process.  This method
    //   allows you to preserve the original MemoryBank.debug value while
    //   turning debugging on for a specific section of code.
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
    // This method develops a variable filename that depends on
    // the type of calendar that is sent in (one of Year, Month,
    // or specific Date). Used in saving of Calendar-based data files.
    // It is kept here (for now?) as opposed to the CalendarNoteGroup
    // because of the additional calls to two static methods also here.
    // BUT - there is no reason that those two could not also move
    // over there, since this method (and findFilename) is their only 'client'.
    // -----------------------------------------------------------------
    static String makeFilename(GregorianCalendar cal, String which) {
        String FileName = MemoryBank.userDataHome + File.separatorChar;
        FileName += String.valueOf(cal.get(Calendar.YEAR));

        FileName += File.separatorChar;
        FileName += which;

        if (!which.equals("Y")) {
            FileName += getMonthIntString(cal.get(Calendar.MONTH));

            if (!which.equals("M")) {
                int date = cal.get(Calendar.DATE);
                if (date < 10)
                    FileName += "0";
                FileName += String.valueOf(date);
            } // end if not a Month note
        } // end if not a Year note

        FileName += "_" + getTimestamp() + ".json";
        return FileName;
    } // end makeFilename


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
            return b1&b2;
        } // end accept
    } // end class logFileFilter


    static TreePath getPath(TreeNode treeNode) {
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

    // ---------------------------------------------------------------------------------
    // Method Name: loadNoteGroupData
    //
    // This is a 'generic' NoteData loader that can handle the loading of data
    //   for ANY class that is a generation of NoteGroup.  This static method
    //   helps separate the load of the data from the various components and
    //   methods that act upon it.
    // ---------------------------------------------------------------------------------
    static Object[] loadNoteGroupData(String theFilename) {
        // theFilename string needs to be the full path to the file.
        return loadNoteGroupData(new File(theFilename));
    }

    static Object[] loadNoteGroupData(File theFile) {
        Object[] theGroup = null;
        try {
            String text = FileUtils.readFileToString(theFile, StandardCharsets.UTF_8.name());
            theGroup = mapper.readValue(text, Object[].class);
            //System.out.println("NoteGroup data from JSON file: " + AppUtil.toJsonString(theGroup));
        } catch (FileNotFoundException fnfe) { // This is allowed, but you get back a null.
        } catch (IOException ex) {
            ex.printStackTrace();
        } // end try/catch
        return theGroup;
    }


    //---------------------------------------------------------------
    // Method Name: addNote
    //
    // Add a note to a Group .
    // A data file for the Group may or may not already exist.
    // Note that this is a static method, meaning that it can be called
    // from any other context without instantiating a group, and that
    // the file being added to MAY NOT BE showing or currently loaded
    // anywhere.  This is why the call to saveData() is needed every time.
    // Even if there is an ongoing method processing several notes, it
    // will still call this method one at a time and we will do a save
    // with each and every call.
    //
    // Two known calling contexts: TodoNoteComponent and EventNoteGroup,
    //   in order to add a note to a Day.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean addNote(String theFilename, NoteData nd) {
        Object[] theGroup;

        // Note that a full file read and write IS needed; we cannot simply do an 'append' because
        // the new note needs to be inserted into an encapsulated ArrayList.
        // However, a database based methodology would not have that same restriction.
        // (note to self, for future upgrade).
        theGroup = loadNoteGroupData(theFilename);

        // No pre-existing data file is ok in this case; we'll just make one.
        if(theGroup == null) {
            ArrayList al = new ArrayList(); // reason for suppressing the 'rawtypes' warning.
            theGroup = new Object[1]; // Only a DayNoteGroup gets notes added this way; no properties.
            theGroup[0] = al;
        }

        // Now here is the cool part - we don't actually need to get the loaded data into a Vector
        // of a specific type (even though we know that the elements are all DayNoteData); we can
        // just add the note to the array of LinkedHashMap.
        ((ArrayList) theGroup[0]).add(nd); // reason for suppressing the 'unchecked' warning.

        int notesWritten = saveNoteGroupData(theFilename, theGroup);
        return notesWritten >= 1;
    } // end addNote

    // ---------------------------------------------------------------------------------
    // Method Name: saveNoteGroupData
    //
    // This static method is needed to separate the writing of the data to a file,
    // from the various components and methods that display and modify it.
    // ---------------------------------------------------------------------------------
    static int saveNoteGroupData(String theFilename, Object[] theGroup) {
        int notesWritten = 0;
        BufferedWriter bw = null;
        Exception e = null;
        try {
            FileOutputStream fileStream = new FileOutputStream(new File(theFilename));
            OutputStreamWriter writer = new OutputStreamWriter(fileStream, StandardCharsets.UTF_8);
            bw = new BufferedWriter(writer);
            bw.write(toJsonString(theGroup));
            // Set the number of notes written, only AFTER the write.
            notesWritten = ((List) theGroup[theGroup.length-1]).size();
        } catch (IOException ioe) {
            // This is a catch-all for other problems that may arise, such as finding a subdirectory of the
            // same name in the directory where you want to put the file, or not having write permission.
            e = ioe;
        } finally {
            if (e != null) {
                // This one may have been ignorable; print the message and see.
                System.out.println("Exception: " + e.getMessage());
            } // end if there was an exception
            // These flush/close lines may seem like overkill, but there is internet support for being so cautious.
            try {
                if (bw != null) {
                    bw.flush();
                    bw.close(); // Also closes the wrapped FileWriter
                }
            } catch (IOException ioe) {
                // This one would be more serious - raise a 'louder' alarm.
                ioe.printStackTrace(System.err);
            } // end try/catch
        } // end try/catch

        return notesWritten;
    }

    static String makeTimeString(Date theDateWithTime) {
        StringBuilder time = new StringBuilder(); // build the return string here.

        // Re-instantiate the calendar to use; it will pick up any new default TimeZone.
        calTemp = (GregorianCalendar) Calendar.getInstance();
        calTemp.setGregorianChange(new GregorianCalendar(1752, Calendar.SEPTEMBER,
                14).getTime());

        calTemp.setTime(theDateWithTime);

        // Hour portion
        int hours = calTemp.get(Calendar.HOUR_OF_DAY);
        if(hours <10) time.append("0");
        time.append(String.valueOf(hours));

        // Separator
        time.append(":");

        // Minutes portion
        int minutes = calTemp.get(Calendar.MINUTE);
        if(minutes <10) time.append("0");
        time.append(String.valueOf(minutes));

        return time.toString();
    } // end makeTimeString


} // end class AppUtil
