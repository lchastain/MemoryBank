import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.runner.RunWith;

// NOTE TO SELF:  Suites are not useful for coverage - when it changes directories the coverage data
//   is getting reset, so you don't get the cumulative value.  If you save it as a run configuration
//   and change the Test Kind from Class to 'All in Package', the name of your configuration changes
//   as well, to 'All in Memory Bank', and the Suite is not used but you do get the overall coverage
//   you would be looking for.

// So a suite seems only useful if for some reason you only want to run a subset of all your tests
// and in that case you shouldn't ask for coverage.  So far - haven't got that reason.


@SuppressWarnings("JUnit5Platform")
@RunWith(JUnitPlatform.class)
@SelectClasses({
        AppSplashTest.class,
        AppTreePanelTest.class,
        DayNoteComponentTest.class,
        DayNoteGroupTest.class,
        IconFileViewTest.class,
        IconNoteComponentTest.class,
        NoteDataTest.class,
        TodoNoteGroup.class,

        AddTodoListTest.class,
        ClearNoteGroupTest.class,
        ClearTodoItemTest.class,
        PreserveExpansionStatesTest.class,
        ToggleAboutTest.class
})
public class AllTestsSuite {
    // This class remains empty, it is used only as a holder for the above annotations
}

