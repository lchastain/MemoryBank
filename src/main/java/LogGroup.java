import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

public class LogGroup extends NoteGroup {

    LogGroup(GroupInfo groupInfo) {
        super(groupInfo); // this sets 'myGroupInfo', in the base.
    }

    @Override  // This will be cast-able to a LogGroupProperties.
    GroupProperties makeGroupProperties() {
        return new LogGroupProperties(myGroupInfo.getGroupName());
    }


    @Override
    protected void setGroupProperties(Object propertiesObject) {
        myProperties = AppUtil.mapper.convertValue(propertiesObject, GoalGroupProperties.class);
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
