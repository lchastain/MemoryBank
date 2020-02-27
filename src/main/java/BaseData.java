import java.time.ZonedDateTime;
import java.util.UUID;

// Intent now (future, not sure how far out) - make a Properties class for each of the other NoteGroup
// types, that inherits from this class (except for GoalGroupProperties, that already inherits from here),
// so their IDs and LMD can be preserved.  (EventNoteGroup and Calendar groups have no properties, but probably should)
// Maybe not that far out, if the ID is needed to 'fix' stale linked data for these
// other groups, the way it is intended for Goals.

public class BaseData {
    static boolean loading = false;
    protected String zdtLastModString;
    protected UUID noteId;    // Now that this has moved up from NoteData, a better name would be 'instanceId', but that would call for a new DataFix.

    BaseData() {
        noteId = UUID.randomUUID();
        if(!loading) {
            zdtLastModString = ZonedDateTime.now().toString();
        }
    }
}
