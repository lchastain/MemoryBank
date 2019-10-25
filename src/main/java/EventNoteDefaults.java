import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class EventNoteDefaults {
    private static String defaultFileName = "EventNoteDefaults.json";

    String defaultIconFileName;

    // Expectation is that additional members will be needed.
    // It will be better to add them at that time rather than
    // making placeholders for them now, because additions do
    // not cause JSON deserialization issues, whereas renames
    // or deletions from the class will result in exceptions
    // when loading data that was saved with the earlier class version.

    // Possible additions:  Date/Time format patterns in String fields.

    public EventNoteDefaults() {
        defaultIconFileName = "icons" + File.separatorChar + "reminder.gif";
    }

    public boolean load() {
        String fileName = MemoryBank.userDataHome + File.separatorChar + defaultFileName;
        Exception e = null;

        try {
            String text = FileUtils.readFileToString(new File(fileName), StandardCharsets.UTF_8.name());
            EventNoteDefaults fromFile = AppUtil.mapper.readValue(text, EventNoteDefaults.class);
            defaultIconFileName = fromFile.defaultIconFileName; // must be a better way, here.
            System.out.println("EventNoteDefaults from JSON file: " + AppUtil.toJsonString(this));
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
        MemoryBank.debug("Saving EventNoteDefaults in " + fileName);

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
