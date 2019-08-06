import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.io.File;

public class IconFileViewTest {
    IconFileView ifv;

    @Before
    public void setUp() throws Exception {
        ifv = new IconFileView();
    }

    @After
    public void tearDown() throws Exception {
        ifv = null;
    }

    @Test
    public void testGetName() throws Exception {
        String fileName = "IconFileViewTest/specs.ico";
        File testFile = FileUtils.toFile(getClass().getResource(fileName));

        assert testFile.exists();
        String aname = ifv.getName(testFile);
        assert !aname.contains(".");
    }

    @Test
    public void testIsTraversable() throws Exception {
        Boolean b = ifv.isTraversable(new File("nofile"));
        assert b == null;
    }

    @Test
    public void testGetDescription() throws Exception {
        String s = ifv.getDescription(new File("nofile"));
        assert s == null;
    }

    @Test
    public void testGetTypeDescription() throws Exception {
        String s = ifv.getTypeDescription(new File("nofile"));
        assert s == null;
    }

    @Test
    public void testGetIcon() throws Exception {
        String fileName = "IconFileViewTest/specs.ico";
        File testFile = FileUtils.toFile(getClass().getResource(fileName));

        assert testFile.exists();
        Icon theIcon = ifv.getIcon(testFile);
        assert theIcon != null;
    }

}