// This class is for stating a connection from a NoteData to another entity -
// either an entire NoteGroup or a single NoteData.  The linkage will be stored
// in the NoteData of the item that is being linked from, and its content will
// be the entity that it is being linked to and the type of relationship.

import java.util.UUID;

public class Linkage {
    public static final int UNSPECIFIED = 0;
    public static final int DEPENDS_ON = 1;
    public static final int DEPENDED_ON_BY = 2;

    String theGroup; // NoteGroup name (includes the prefix)
    UUID theId;      // ID of the linked note.  If null then the linkage is to the entire group (usually a Goal).
    int theType;     // Values defined above
}
