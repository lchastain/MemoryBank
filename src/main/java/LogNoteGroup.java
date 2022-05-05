import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

// A LogNoteGroup can be associated to a 'parent' NoteGroup (currently only the GoalGroupPanel has a LogNoteGroupPanel).
// The association is defined by the name of the Group and the location of the data rather than being contained in the
//   properties of the Group.  For example, when the data accessor is set to the local filesystem, a Log for the Goal
//   'Retire' will be stored in the same location as the Goal but will be named 'log_Retire'.
public class LogNoteGroup extends NoteGroup {

    LogNoteGroup(GroupInfo groupInfo) {
        super(groupInfo); // this sets 'myGroupInfo', in the base.
    }

    @Override  // This return type will be cast-able to a LogGroupProperties.
    GroupProperties makeGroupProperties() {
        return new LogGroupProperties(myGroupInfo.getGroupName());
    }


    @Override
    protected void setGroupProperties(Object propertiesObject) {
        myProperties = AppUtil.mapper.convertValue(propertiesObject, LogGroupProperties.class);
    }

    @Override
    protected void setNotes(Object vectorObject) {
        if(vectorObject instanceof Vector) {
            noteGroupDataVector = (Vector) vectorObject;
        } else {
            noteGroupDataVector = AppUtil.mapper.convertValue(vectorObject, new TypeReference<Vector<LogNoteData>>() {
            });
        }
    }
}
