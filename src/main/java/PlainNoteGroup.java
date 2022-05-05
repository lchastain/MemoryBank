import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

// A PlainNoteGroup can be either a standalone Panel, or it can be associated to a 'parent' NoteGroup (currently only
//   the GoalGroupPanel has one).  The association is defined by the name of the Group and the location of the data
//   rather than being contained in the properties of the Group.  For example, when the data accessor is set to the
//   local filesystem, the Notes for the Goal 'Retire' will be stored in the same location as the Goal but will be
//   named 'notes_Retire'.
public class PlainNoteGroup extends NoteGroup {

    PlainNoteGroup(GroupInfo groupInfo) {
        super(groupInfo); // this sets 'myGroupInfo', in the base.
    }

    @Override
    protected void setNotes(Object vectorObject) {
        if (vectorObject == null) {
            noteGroupDataVector.clear(); // null not allowed here.
        } else if (vectorObject instanceof Vector) {
            noteGroupDataVector = (Vector) vectorObject;
        } else {
            noteGroupDataVector = AppUtil.mapper.convertValue(vectorObject, new TypeReference<Vector<NoteData>>() {
            });
        }
    }
}
