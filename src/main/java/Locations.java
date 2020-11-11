import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

public class Locations {
    private static String locationsFilename = "Locations.json";
    private static final int maxLocations = 20;

    Vector<String> shortNames;

    // Expectation is that additional members may be needed.
    // It will be better to add them at that time rather than
    // making placeholders for them now, because additions do
    // not cause JSON deserialization issues, whereas renames
    // or deletions from the class will result in exceptions
    // when loading data that was saved with the earlier class version.

    // Possible additions:  Longitude/Latitude, addresses, timezones, etc.
    // IF that happens, will need a new class: Location, with those members
    // and this class will just become a list of those.

    public Locations() {
        shortNames = new Vector<>(6, 1);
        shortNames.add("Work");
        shortNames.add("Home");
        shortNames.add("Office");
        shortNames.add("1600 Pennsylvania Avenue NW, Washington, DC 20500");
    }

    void add(String s) {
        MemoryBank.debug("Adding location: [" + s + "]");
        if (s.equals("")) return;

        //------------------------------------------------------------------
        // Check to see if this location is already first in the list -
        //------------------------------------------------------------------
        if (shortNames.size() > 0) {
            if ((shortNames.elementAt(0)).equals(s)) return;
        } // end if

        //------------------------------------------------------------------
        // Then, remove an occurrence of this subject lower in the list.
        //------------------------------------------------------------------
        shortNames.remove(s);

        //------------------------------------------------------------------
        // Then, put this subject at the top of the list.
        //------------------------------------------------------------------
        shortNames.insertElementAt(s, 0);

        //------------------------------------------------------------------
        // Then, if the list has grown too big, truncate.
        //------------------------------------------------------------------
        if (shortNames.size() > maxLocations) {
            shortNames.remove(shortNames.lastElement());
        } // end if too many
    } // end addLocation


    public static Locations load() {
        String fileName = MemoryBank.userDataHome + File.separatorChar + locationsFilename;
        Exception e = null;

        try {
            String text = FileUtils.readFileToString(new File(fileName), StandardCharsets.UTF_8.name());
            Locations fromFile = AppUtil.mapper.readValue(text, Locations.class);
            System.out.println("Locations from JSON file: " + AppUtil.toJsonString(fromFile));
            return fromFile;
        } catch (Exception ignore) {
        }  // not a big problem; use defaults.  When trying to cause Exceptions for Testing by making
        // 'bad' filenames, found that none get through the FileUtils; the only Exception I could get
        // was FileNotFound, and that one is effectively 'allowed' anyway, so stopped trying to handle
        // any of them and just take any unhappy path as needing the default handling.
        MemoryBank.debug("Locations not found; using defaults");
        return new Locations();
    }

    public boolean save() {
        String fileName = MemoryBank.userDataHome + File.separatorChar + locationsFilename;
        MemoryBank.debug("Saving Locations in " + fileName);

        try (FileWriter writer = new FileWriter(fileName);
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(AppUtil.toJsonString(this));
            bw.flush();
        } catch (IOException ioe) {
            String ems = ioe.getMessage();
            ems = ems + "\nLocations save operation aborted.";
            MemoryBank.debug(ems);
            return false;
        } // end try/catch

        return true;
    }
}
