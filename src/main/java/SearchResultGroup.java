import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

public class SearchResultGroup extends NoteGroup {

    SearchResultGroup(GroupInfo groupInfo) {
        super(groupInfo); // this sets 'myGroupInfo', in the base.
    }

    @Override
    GroupProperties makeGroupProperties() {
        return new SearchResultGroupProperties(myGroupInfo.getGroupName());
    }


    @Override
    protected void setGroupProperties(Object propertiesObject) {
        myProperties = AppUtil.mapper.convertValue(propertiesObject, SearchResultGroupProperties.class);
    }

    @Override
    protected void setNotes(Object vectorObject) {
        if(vectorObject instanceof Vector) {
            noteGroupDataVector = (Vector) vectorObject;
        } else {
            noteGroupDataVector = AppUtil.mapper.convertValue(vectorObject, new TypeReference<Vector<SearchResultData>>() {
            });
        }
    }

}
