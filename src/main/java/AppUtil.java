import java.io.*;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class AppUtil {

    static GregorianCalendar calTemp;

    public static SimpleDateFormat sdf;

    private static Boolean blnGlobalArchive;

    private static Boolean blnGlobalDebug;

    // This is a global flag that Test methods can check, to see if the defalt Notifier
    // should be replaced with one that does not wait for user interaction with a JOptionPane.
    // Reason to do that: so that all tests can run without user interaction.
    // Reason to not do that: maximize test coverage.
    // This flag is changed manually as needed for the desired effect, with Tests running afterwards.
    static Boolean blnReplaceNotifiers = true;

    static {
        sdf = new SimpleDateFormat();
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
    // will return the appropriate filename if it exists and is
    // unique for the indicated timeframe.
    // If no file exists, the return string is empty ("").
    // -----------------------------------------------------------------
    static String findFilename(GregorianCalendar cal, String which) {
        String foundFiles[] = null;
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
    public static Date getDateFromFilename(File f) {
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
        String strTheBrokenString;
        String strNextLine;

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

        byte aryTheChars[] = s.getBytes();

        // Look through the character array and
        // insert line breaks as needed.
        strTheBrokenString = "";
        strNextLine = "";
        intLastBreak = -1;
        intCurrentCharCount = 0;
        for (i = 0; i < intLength; i++) {
            // Get the next char.
            intCurrentCharCount++;

            // Did we encounter a linefeed ?
            if (aryTheChars[i] == '\n') {
                // First, take the line up to (not including) the lf.
                strTheBrokenString += strNextLine;
                strNextLine = "";

                // Now, bail if we're at the line limit.
                if ((l > 0) && (intLineCount >= l))
                    break;

                // Otherwise, add the lf
                strTheBrokenString += '\n';
                intLineCount++;

                // And reset the counts
                intCurrentCharCount = 0;
                intLastBreak = -1;
            } else {
                strNextLine += (char) aryTheChars[i];
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
                    strTheBrokenString += strNextLine.substring(0, intLastBreak + 1);
                    strNextLine = strNextLine.substring(intLastBreak + 1);
                    intLastBreak = -1;
                    intCurrentCharCount = strNextLine.length();
                } else {
                    // The string has no logical breaks; just break here.
                    strTheBrokenString += strNextLine.substring(0, n + 1);
                    strNextLine = strNextLine.substring(n + 1); // one char
                    intCurrentCharCount = strNextLine.length();
                } // end if we had a good place to break the line

                // Now, bail if we're at the line limit.
                if ((l > 0) && (intLineCount >= l)) {
                    strNextLine = "";
                    break;
                }

                // Otherwise, insert an lf and keep going
                intLineCount++;
                strTheBrokenString += '\n';
            } // end if we need to break the line
        } // end for i

        // Take the remaining last partial line, if any.
        strTheBrokenString += strNextLine;

        // System.out.println("Final line count: " + intLineCount);

        return strTheBrokenString;
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
    public static String getTimestamp() {
        String s;

        calTemp.setTime(new Date());

        int year = calTemp.get(Calendar.YEAR);
        s = String.valueOf(year);

        // This should not be necessary unless the system time is hosed...
        while (s.length() < 4) {
            s = "0" + s;
        } // end while

        s += getMonthIntString(calTemp.get(Calendar.MONTH));

        int date = calTemp.get(Calendar.DATE);
        if (date < 10)
            s += "0";
        s += String.valueOf(date);

        int hour = calTemp.get(Calendar.HOUR_OF_DAY);
        if (hour < 10)
            s += "0";
        s += String.valueOf(hour);

        int minute = calTemp.get(Calendar.MINUTE);
        if (minute < 10)
            s += "0";
        s += String.valueOf(minute);

        int second = calTemp.get(Calendar.SECOND);
        if (second < 10)
            s += "0";
        s += String.valueOf(second);

        return s;
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
    public static void localArchive(boolean b) {
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

        FileName += "_" + getTimestamp();
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
            boolean b2 = !name.endsWith(".json");
            return b1&b2;
        } // end accept
    } // end class logFileFilter

} // end class AppUtil
