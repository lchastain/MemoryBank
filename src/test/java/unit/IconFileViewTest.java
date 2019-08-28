import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.File;

class IconFileViewTest {
    private IconFileView ifv;

    @BeforeEach
    void setUp() {
        ifv = new IconFileView();
    }

    @AfterEach
    void tearDown() throws Exception {
        ifv = null;
    }

    @Test
    void testGetName() {
        String fileName = "IconFileViewTest/specs.ico";
        File testFile = FileUtils.toFile(getClass().getResource(fileName));

        assert testFile.exists();
        String aname = ifv.getName(testFile);
        assert !aname.contains(".");
    }

    @Test
    void testIsTraversable() {
        // The provided File does not matter; we want a null in every case.
        Boolean b = ifv.isTraversable(new File("nofile"));
        assert b == null;
    }

    @Test
    void testGetDescription() {
        // The provided File does not matter; we want a null in every case.
        String s = ifv.getDescription(new File("nofile"));
        assert s == null;
    }

    @Test
    void testGetTypeDescription() {
        // The provided File does not matter; we want a null in every case.
        String s = ifv.getTypeDescription(new File("nofile"));
        assert s == null;
    }

    @Test
    void testGetIcon() {
        String fileName = "IconFileViewTest/specs.ico";
        File testFile = FileUtils.toFile(getClass().getResource(fileName));

        assert testFile.exists();
        Icon theIcon = ifv.getIcon(testFile);
        assert theIcon != null;
    }

}