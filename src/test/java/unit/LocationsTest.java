import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocationsTest {
    private Locations locations;

    @BeforeEach
    void setUp() {
        MemoryBank.debug = true;
        locations = new Locations();
    }

    @Test
    void testAdd() {
        locations.add("the basement01");
        locations.add("the basement02");
        locations.add("the basement03");
        locations.add("the basement04");
        locations.add("the basement05");
        locations.add("the basement06");
        locations.add("the basement07");
        locations.add("the basement08");
        locations.add("the basement09");
        locations.add("the basement10");
        locations.add("the basement11");
        locations.add("the basement12");
        locations.add("the basement13");
        locations.add("the basement14");
        locations.add("the basement15");
        locations.add("the basement16");
        locations.add("the basement17");
    }

    @Test
    void testLoad() {
        MemoryBank.setUserDataHome(null);
        locations = Locations.load();  // The bad one
        MemoryBank.setUserDataHome("test.user@lcware.net");
        locations = Locations.load();  // The good one
    }

    @Test
    void testSave() {
        MemoryBank.setUserDataHome(null);
        locations.save();
        MemoryBank.setUserDataHome("test.user@lcware.net");
        locations.save();
    }
}