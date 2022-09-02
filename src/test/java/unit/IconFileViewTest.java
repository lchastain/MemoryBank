import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

//@Disabled("Explicitly Disabled")
class IconFileViewTest {
    private IconFileView ifv;

    @BeforeAll
    static void beforeAll() throws IOException {
        MemoryBank.debug = true;
        MemoryBank.appEnvironment = "ide";
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
    }

    @BeforeEach
    void setUp() {
        ifv = new IconFileView();
    }

    @AfterEach
    void tearDown() {
        ifv = null;
    }

    @Test
//    @Disabled
    void testGetName() {
        String fileName = "IconFileViewTest/specs.ico";
        File testFile = FileUtils.toFile(getClass().getResource(fileName));
        assert testFile != null;
        assert testFile.exists();
        String aname = ifv.getName(testFile);
        assert !aname.contains(".");
    }

    @Test
//    @Disabled
    void testIsTraversable() {
        // The provided File does not matter; we want a null in every case.
        Boolean b = ifv.isTraversable(new File("nofile"));
        assert b == null;
    }

    @Test
//    @Disabled
    void testGetDescription() {
        // The provided File does not matter; we want a null in every case.
        String s = ifv.getDescription(new File("nofile"));
        assert s == null;
    }

    @Test
//    @Disabled
    void testGetTypeDescription() {
        // The provided File does not matter; we want a null in every case.
        String s = ifv.getTypeDescription(new File("nofile"));
        assert s == null;
    }

    @Test
//    @Disabled
    void testGetIcon() {
        String fileName = "IconFileViewTest/specs.ico";
        File testFile = FileUtils.toFile(getClass().getResource(fileName));

        assert testFile != null;
        Assertions.assertTrue(testFile.exists());
        Icon theIcon = ifv.getIcon(testFile);
        assert theIcon != null;
    }

}