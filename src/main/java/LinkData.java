import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.UUID;

// This class is for creating a connection from a NoteData instance to another entity -
// either an entire NoteGroup or another single NoteData.  The linkage will be stored
// in the NoteData.linkages list of the NoteData that is being linked from, and it will
// identify the entity that the NoteData is being linked to and the type of relationship
// that exists from the perspective of the source NoteData (not the target).

// Most NoteData instances can be linked to more than one entity; that is why they define
// a List of linkages vs a single one.

// SearchResultData will not be linked to any other note or group, because doing so would make it
//   appear as though there are duplicates, when viewing the linkages of the various targets.
//   And, a SearchResult has a 'FoundInFile' field that is similar to a LinkData in that it can be
//   used to get to the original note, and THAT one can be linked to some other Note or Group.


public class LinkData extends IconNoteData {
    // LinkData extends IconNoteData so that it picks up the icon handling from that class, but
    //   for practical purposes it is extending the base class - NoteData.  The reason for this
    //   is so that GoalGroup can manage a Vector of these links in the same way that all the other
    //   NoteGroups manage their Vectors, mostly handled by the base NoteGroup.  Otherwise the GoalGroup
    //   would have to handle a new data type during interface loading/unloading, component management,
    //   and group data persistence.  The tradeoff is that we are double-storing most of the LinkData
    //   members we inherit, but even that downside has potential benefits - if the target of the link is
    //   a GoalGroup then the links are also being kept in that Goal, so that if the source Note has been
    //   moved or removed then the Goal still will be able to display the information.

    // In a LinkData instance, all the inherited NoteData members are populated with the data
    //   from the NoteData that the link is pointing from.  Beyond that, it needs to define its
    //   own members which will describe the link itself.  Those are defined below.

    // One data member below is inherited from NoteData and then overloaded & ignored: linkages.
    //   This is so that NoteData instances may be serialized without infinite recursion, given that
    //     NoteData's linkages list is made up of instances of this LinkData class, which itself
    //     would otherwise have a linkages list made up of instances of this LinkData class, which...
    //  For instances of this class, the linkages list will remain null, unused and ignored.

    //  Other inherited members such as noteString, subjectString and extendedNoteString - will be
    //     populated with the source NoteData info and they do not pose a serialization problem.
    //
    //  The other member - noteId - is not used to identify the LinkData itself; in the case where
    //     it is being created to point to another NoteData or NoteGroup then the ID is overwritten
    //     with the ID of that target entity.  But when a LinkData is added to a GoalGroup, the ID
    //     is overwritten with the ID of the source NoteData.  Most linkages are one-way but a link
    //     to a Goal is bi-directional, so there are two separate LinkData instances.
    //------------------------------------------------------------------------------------------
    @JsonIgnore ArrayList linkages;  // Reason for overloading and then ignoring: explained above.
    String theGroup; // Name of the NoteGroup where the linked NoteData resides (includes the prefix).
    private UUID targetId;   // The ID of the target entity.  Can be null if the target has no ID (some Groups do not).
    int theType;     // Says what kind of connection this is.  Values defined below.
    // status - or 'order' / priority

    // Type Values:
    public static final int UNSPECIFIED = 0;
    public static final int DEPENDS_ON = 1;
    public static final int DEPENDED_ON_BY = 2;


    public LinkData() {
        super(); // This gives us our 'own' ID, which we don't need.
        theType = UNSPECIFIED;
    }

    public LinkData(NoteData noteData) {
        super();
        this.noteId = noteData.noteId;
        this.zdtLastModString = noteData.zdtLastModString;
        this.noteString = noteData.noteString;
        this.subjectString = noteData.subjectString;
        this.extendedNoteString = noteData.extendedNoteString;
    }

    void setTargetId(UUID theTargetId) {
        targetId = theTargetId;
    }

    UUID getTargetId() { return targetId; }

}
