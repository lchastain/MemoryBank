import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class DayNoteDefaults {
    private static String defaultFileName = "DayNoteDefaults.json";

    String defaultIconFileName;

    // Expectation is that additional members will be needed.
    // It will be better to add them at that time rather than
    // making placeholders for them now, because additions do
    // not cause JSON deserialization issues, whereas renames
    // or deletions from the class will result in exceptions
    // when loading data that was saved with the earlier class version.

    // Possible additions:  Time format pattern in a String field.
    //    and moving the 'military' boolean to here from MemoryBank.

    public DayNoteDefaults() {
        defaultIconFileName = "icons" + File.separatorChar + "icon_not.gif";
    }

    public boolean load() {
        String fileName = MemoryBank.userDataHome + File.separatorChar + defaultFileName;
        Exception e = null;

        try {
            String text = FileUtils.readFileToString(new File(fileName), StandardCharsets.UTF_8.name());
            DayNoteDefaults fromFile = AppUtil.mapper.readValue(text, DayNoteDefaults.class);
            defaultIconFileName = fromFile.defaultIconFileName; // must be a better way, here.
            System.out.println("DayNoteDefaults from JSON file: " + AppUtil.toJsonString(this));
        } catch (FileNotFoundException fnfe) {
            // not a problem; use defaults.
            MemoryBank.debug("User tree options not found; using defaults");
        } catch (IOException ioe) {
            e = ioe;
            e.printStackTrace();
        }

        if (e != null) {
            String ems = "Error in loading " + fileName + " !\n";
            ems = ems + e.toString();
            ems = ems + "\noperation failed; using default values.";
            MemoryBank.debug(ems);
            return false;
        } // end if
        return true;
    }

    public boolean save() {
        String fileName = MemoryBank.userDataHome + File.separatorChar + defaultFileName;
        MemoryBank.debug("Saving DayNoteDefaults in " + fileName);

        try (FileWriter writer = new FileWriter(fileName);
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(AppUtil.toJsonString(this));
            bw.flush();
        } catch (IOException ioe) {
            String ems = ioe.getMessage();
            ems = ems + "\nDayNoteDefaults save operation aborted.";
            MemoryBank.debug(ems);
            return false;
        } // end try/catch

        return true;
    }
}
