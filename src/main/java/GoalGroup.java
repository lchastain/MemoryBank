

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
    // In this case we do not have any notes to set.
    protected void setNotes(Object vectorObject) {
    }

}
