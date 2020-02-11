import java.time.ZonedDateTime;
import java.util.Vector;

// From the base class -
// noteString will be used as the Goal statement / title.
// extendedNoteString will hold the Plan.

public class Goal extends NoteData {
    private String goalFilename;
    Vector planSteps;  // discrete tasks, events, notes
    int goalStatus;
    Vector goalLog;

    static boolean loading = false;

    // We don't call super here because we don't need the UUID
    public Goal() {
        if(!loading) {
            zdtLastModString = ZonedDateTime.now().toString();
        }
        clear();
    }

    // base class all that's needed?
//    void clear() {
//        goalStatement = "";
//    } // end clear



}
