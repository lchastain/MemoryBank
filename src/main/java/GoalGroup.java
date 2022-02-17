

public class GoalGroup extends NoteGroup {

    GoalGroup(GroupInfo groupInfo) {
        super(groupInfo); // this sets 'myGroupInfo', in the base.
    }

    @Override
    GroupProperties makeGroupProperties() {
        return new GoalGroupProperties(myGroupInfo.getGroupName());
    }


    @Override
    void renameNoteGroup(String renameTo) {
        if(myNoteGroupPanel == null) {
            // Need to have a GoalPanel with the original name, so that the rename can cascade thru to its tabs.
            myNoteGroupPanel = new GoalGroupPanel(myGroupInfo.getGroupName());
        }
        super.renameNoteGroup(renameTo);
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
