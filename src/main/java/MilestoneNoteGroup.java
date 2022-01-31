import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

// No change in Properties from the base, but we still need to set a note type.
public class MilestoneNoteGroup extends NoteGroup {

    MilestoneNoteGroup(GroupInfo groupInfo) {
        super(groupInfo); // this sets 'myGroupInfo', in the base.
    }

    @Override
    protected void setNotes(Object vectorObject) {
        if(vectorObject instanceof Vector) {
            noteGroupDataVector = (Vector) vectorObject;
        } else {
            noteGroupDataVector = AppUtil.mapper.convertValue(vectorObject, new TypeReference<Vector<MilestoneNoteData>>() {
            });
        }
    }

}
