import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

public class TodoNoteGroup extends NoteGroup {

    TodoNoteGroup(GroupInfo groupInfo) {
        super(groupInfo); // this sets 'myGroupInfo', in the base.
    }

    @Override
    GroupProperties makeGroupProperties() {
        return new TodoGroupProperties(myGroupInfo);
    }


    @Override
    protected void setGroupProperties(Object propertiesObject) {
        myProperties = AppUtil.mapper.convertValue(propertiesObject, TodoGroupProperties.class);
    }

    @Override
    protected void setNotes(Object vectorObject) {
        if(vectorObject instanceof Vector) {
            noteGroupDataVector = (Vector) vectorObject;
        } else {
            noteGroupDataVector = AppUtil.mapper.convertValue(vectorObject, new TypeReference<Vector<TodoNoteData>>() {
            });
        }
    }

}
