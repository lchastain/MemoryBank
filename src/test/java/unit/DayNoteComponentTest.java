import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

class DayNoteComponentTest {
    DayNoteComponent dnc;

    @BeforeEach
    void setUp() {
        dnc = new DayNoteComponent(new DayNoteGroup() {
            @Override
            public String getGroupFilename() {
                return "TestNoteGroup";
            }
        }, 0);
        dnc.initialize();
    }

    @AfterEach
    void tearDown() throws Exception {
        dnc = null;
        Thread.sleep(100); // allow some time for GC
    }

    @Test
    void clear() {
        dnc.clear();
    }

//    @Test
//    void getNoteData() {
//    }
//
//    @Test
//    void getPreferredSize() {
//    }
//
//    @Test
//    void initialize() {
//    }
//
//    @Test
//    void makeDataObject() {
//    }
//
//    @Test
//    void noteActivated() {
//    }
//
//    @Test
//    void resetComponent() {
//    }

    @Test
    void setIcon() {
        String fileName = "IconFileViewTest/specs.ico";
        File testFile = FileUtils.toFile(getClass().getResource(fileName));

        dnc.setIcon(new AppIcon(testFile.getPath()));
    }

//    @Test
//    void resetTimeLabel() {
//    }
//
//    @Test
//    void setNoteData() {
//    }
//
//    @Test
//    void testSetNoteData() {
//    }
//
//    @Test
//    void shiftDown() {
//    }
//
//    @Test
//    void shiftUp() {
//    }
//
//    @Test
//    void swap() {
//    }
}