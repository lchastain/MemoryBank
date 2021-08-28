import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

public class EventNoteGroup extends NoteGroup {

    EventNoteGroup(GroupInfo groupInfo) {
        super(groupInfo); // this sets 'myGroupInfo', in the base.
    }

    @Override
    protected void setNotes(Object vectorObject) {
        if(vectorObject instanceof Vector) {
            noteGroupDataVector = (Vector) vectorObject;
        } else {
            noteGroupDataVector = AppUtil.mapper.convertValue(vectorObject, new TypeReference<Vector<EventNoteData>>() {
            });
        }
    }

}
