import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

public class GoalGroup extends NoteGroup {

    GoalGroup(GroupInfo groupInfo) {
        super(groupInfo); // this sets 'myGroupInfo', in the base.
    }

    @Override
    GroupProperties makeGroupProperties() {
        return new GoalGroupProperties(myGroupInfo.getGroupName());
    }


    @Override
    protected void setGroupProperties(Object propertiesObject) {
        myProperties = AppUtil.mapper.convertValue(propertiesObject, GoalGroupProperties.class);
    }

    @Override
    protected void setNotes(Object vectorObject) {
        // This type will need to change, ultimately.  Need a round tuit.
        noteGroupDataVector = AppUtil.mapper.convertValue(vectorObject, new TypeReference<Vector<TodoNoteData>>() { });
    }

}
