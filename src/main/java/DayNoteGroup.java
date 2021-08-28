import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

public class DayNoteGroup extends CalendarNoteGroup {
    DayNoteGroup(GroupInfo groupInfo) {
        super(groupInfo);
    }


    @Override
    protected void setNotes(Object vectorObject) {
        if(vectorObject instanceof Vector) {
            noteGroupDataVector = (Vector) vectorObject;
        } else {
            noteGroupDataVector = AppUtil.mapper.convertValue(vectorObject, new TypeReference<Vector<DayNoteData>>() {
            });
        }
    }

}
