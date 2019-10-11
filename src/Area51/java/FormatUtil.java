import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

// These methods have been moved around, a lot - in a static class called
// Global as well as the various TimeFormat classes, and then in MemoryBank
// and possibly left active in more than one place.  They still need sorting
// out but currently they are moving out of the active codebase, so we will
// not do that, at this time.  10/11/2019

class FormatUtil {
    private static DateTimeFormatter dtf;

    static String getDateString(long theDateLong, String theFormat) {
        ZonedDateTime aDate = Instant.ofEpochMilli(theDateLong).atZone(ZoneId.systemDefault());
        String s = getRealFormat(theFormat);
        dtf = DateTimeFormatter.ofPattern(s);
        return dtf.format(aDate);
    } // end getDateString

    // Given the known configuration of the initialFormat string,
    //   parse out and return the requested element.
    private static String getSeparatorFromFormat(String initialFormat,
                                                 int i, boolean trim) {
        String s;
        int end = initialFormat.indexOf("|");
        if (end == -1) s = "";
        else s = initialFormat.substring(0, end);

        int numFields = s.length();
        String theField = "";
        int pos = s.indexOf(String.valueOf(i));
        if (pos == -1) return " "; // this field not in the format.

        // Get past the order-specifying prefix of the format string
        String suffix = initialFormat.substring(s.length() + 1);
        // System.out.println("suffix: " + suffix);

        for (int j = 0; j < numFields; j++) {
            if (j == numFields - 1) theField = suffix;  // last field
            else theField = suffix.substring(0, suffix.indexOf("|"));
            if (j == pos) break;
            suffix = suffix.substring(theField.length() + 1);
        } // end for j

        // Got the Field; now process the separator string, if any.
        int sspos = theField.indexOf("'");
        if (sspos == -1) return "";

        String theSeparator = theField.substring(sspos);
        if (!trim) return theSeparator;

        // Trim of the single quotes.
        theSeparator = theSeparator.substring(1);
        theSeparator = theSeparator.substring(0, theSeparator.length() - 1);
        return theSeparator;
    } // end getSeparatorFromFormat


    // Given a Date/Time format that is unique to the MemoryBank program,
    //  return the requested field from that format.
    static String getFieldFromFormat(int i, String initialFormat) {
        String s;
        int end = initialFormat.indexOf("|");
        if (end == -1) s = "";
        else s = initialFormat.substring(0, end);
        // String may contain any of: "134567"

        int numFields = s.length();
        String theField = "";
        int pos = s.indexOf(String.valueOf(i));
        if (pos == -1) return theField; // this field not in the format.
        // System.out.println("Looking for field " + i + " in " + initialFormat);

        // Get past the order-specifying prefix of the format string
        String suffix = initialFormat.substring(s.length() + 1);
        // System.out.println("suffix: " + suffix);

        for (int j = 0; j < numFields; j++) {
            if (j == numFields - 1) theField = suffix;  // last field
            else theField = suffix.substring(0, suffix.indexOf("|"));
            if (j == pos) break;
            suffix = suffix.substring(theField.length() + 1);
        } // end for j

        // The composite Time field may have multiple separators.
        if (i == 5) return theField;

        // Now cut off the separator string, if any.
        int sspos = theField.indexOf("'");
        if (sspos == -1) return theField;
        return theField.substring(0, sspos);
    } // end getFieldFromFormat


    private static String getRealFormat(String theFormat) {
        // System.out.println("Format parsing: [" + theFormat + "]");
        if (theFormat.equals("")) return "";  // never been set
        if (theFormat.equals("0")) return ""; // explicitly set to ""

        StringBuilder s = new StringBuilder();
        int which;
        String theField;

        int end = theFormat.indexOf("|");
        if (end == -1) return "";
        String order = theFormat.substring(0, end);

        for (int i = 0; i < order.length(); i++) {
            which = Integer.parseInt(order.substring(i, i + 1));
            theField = getFieldFromFormat(which, theFormat);
            if (which == 5) {
                s.append(TimeFormatBar.getRealFormat(theField));
            } else {
                s.append(theField);
                s.append(getSeparatorFromFormat(theFormat, which, false));
            } // end if
        } // end for i

        s = new StringBuilder(s.toString().replace("#SQUOTE#", "''"));
        s = new StringBuilder(s.toString().replace("#DQUOTE#", "\""));
        s = new StringBuilder(s.toString().replace("#VBAR#", "|"));
        // System.out.println("The REAL format specifier is: " + s);
        return s.toString();
    } // end getRealFormat


}
