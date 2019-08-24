import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.runner.RunWith;

@SuppressWarnings("JUnit5Platform")
@RunWith(JUnitPlatform.class)
@SelectClasses({AppSplashTest.class, AppTreePanelTest.class, NoteDataTest.class,
                DayNoteComponentTest.class, DayNoteGroupTest.class, IconFileViewTest.class,
                IconNoteComponentTest.class})
public class UnitTestSuite {
    // This class remains empty, it is used only as a holder for the above annotations
}

