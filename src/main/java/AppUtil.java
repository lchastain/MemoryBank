import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.text.WordUtils;

import javax.swing.*;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.FilenameFilter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// This class provides static utility methods that are needed by more than one client-class in the main app.
// It makes more sense to collect them into one utility class, than to try to decide which user class
// should house a given method while the other user classes then have to somehow get access to it.
public class AppUtil {
    static ObjectMapper mapper;

    private static Boolean blnGlobalDebug; // initially null

    static {
        mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    } // end static


    // -------------------------------------------------------------
    // Method Name: getTooltipString
    //
    // This method takes a string and returns it with inserted
    // line breaks, if needed.  Character column and line limits
    // are hardcoded.
    // -------------------------------------------------------------
    static String getTooltipString(String s) {
        String theToolTip = s;
        StyledDocumentData sdd = StyledDocumentData.getStyledDocumentData(s);
        if (sdd != null) {
            JTextPane jtp = new JTextPane();
            DefaultStyledDocument dsd = (DefaultStyledDocument) jtp.getStyledDocument();
            sdd.fillStyledDocument(dsd);
            theToolTip = jtp.getText();
        }

        StringBuilder theTooltipString = new StringBuilder();
        String[] strings = theToolTip.split("\n");
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

        if (MemoryBank.appOpts.timeFormat == AppOptions.TimeFormat.MILITARY) {
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


    // This is my own conversion, to numbers that matched these
    // that were being returned by Calendar queries, now deprecated.
    // I put this in place as a temporary remediation along the way
    // to updating the app to new Java 8 date/time classes.
    // TODO - refactor all usages of this method to use getDayOfWeek, remove this one.
    static int getDayOfWeekInt(LocalDate tmpDate) {
        return switch (tmpDate.getDayOfWeek()) {
            case SUNDAY -> 1;
            case MONDAY -> 2;
            case TUESDAY -> 3;
            case WEDNESDAY -> 4;
            case THURSDAY -> 5;
            case FRIDAY -> 6;
            case SATURDAY -> 7;
        };
    }

} // end class AppUtil
