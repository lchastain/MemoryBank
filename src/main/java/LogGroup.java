import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

// A LogGroup may be either a standalone NoteGroup, or associated to a 'parent' NoteGroup.
// The association is defined by the location of the data and is not to be found within
//   the data itself.  For example, a Log for the Goal 'Retire' will be stored in the same
//   location as the Goal but will be named 'log_Retire'.

public class LogGroup extends NoteGroup {

    LogGroup(GroupInfo groupInfo) {
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
            noteGroupDataVector = AppUtil.mapper.convertValue(vectorObject, new TypeReference<Vector<LogData>>() {
            });
        }
    }
}
