import java.util.Vector;

public class Locations {
    private static final int maxLocations = 20;

    Vector<String> shortNames;

    // Expectation is that additional data members may be needed.
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
        return MemoryBank.dataAccessor.loadLocations();
    }

    public boolean save() {
        return MemoryBank.dataAccessor.saveLocations(this);
    }
}
