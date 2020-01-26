import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class EventNoteDefaults {
    private static String defaultFileName = "EventNoteDefaults.json";

    String defaultIconFileName;

    // Additional members may occasionally be needed.  It will be better to
    // add them at that time rather than having pre-existing placeholders
    // for them, because additions do not cause JSON deserialization issues,
    // whereas renames or deletions from the class will result in exceptions
    // when loading data that was saved with the earlier class version.

    // Possible additions:  Date/Time format patterns in String fields.

    public EventNoteDefaults() {
        defaultIconFileName = "icons" + File.separatorChar + "reminder.gif";
    }

    public static EventNoteDefaults load() {
        String fileName = MemoryBank.userDataHome + File.separatorChar + defaultFileName;
        Exception e = null;

        try {
            String text = FileUtils.readFileToString(new File(fileName), StandardCharsets.UTF_8.name());
            EventNoteDefaults fromFile = AppUtil.mapper.readValue(text, EventNoteDefaults.class);
            System.out.println("EventNoteDefaults from JSON file: " + AppUtil.toJsonString(fromFile));
            return fromFile;
        } catch (FileNotFoundException ignore) { // not a problem; we'll use defaults.
        } catch (IOException ioe) {
            e = ioe;
            if(MemoryBank.debug) e.printStackTrace();
        }

        if (e != null) {
            String ems = "Error in loading " + fileName + " !\n";
            ems = ems + e.toString();
            ems = ems + "\noperation failed; using default values.";
            MemoryBank.debug(ems);
        } // end if
        return new EventNoteDefaults();
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
            ems = ems + "\nEventNoteDefaults save operation aborted.";
            MemoryBank.debug(ems);
            return false;
        } // end try/catch

        return true;
    }
}
