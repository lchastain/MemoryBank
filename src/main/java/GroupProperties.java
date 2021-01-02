// This class holds the persistent metadata for a Group.
// In addition to the ID and LastModDate that it gets from BaseData, it holds
//   a simple group name (String) and the type of group.

import com.fasterxml.jackson.annotation.JsonIgnore;

// We don't want GroupInfo to come from BaseData, yet GroupProperties needs to, so -
// GroupProperties do not extend GroupInfo; the needed members are present in both.

public class GroupProperties extends BaseData {
    GroupType groupType;     // Says what kind of group this is.
    private String groupName; // The name of the group, as shown in the Tree.
    LinkTargets linkTargets;

    @JsonIgnore
    private String simpleName; // A previous version of 'groupName'.  Needs to be removed from all data.

    // This constructor is used by Jackson type conversion operations and child class constructors.
    GroupProperties() { }

    GroupProperties(String theName, GroupType theType) {
        groupName = theName;
        groupType = theType;
        linkTargets = new LinkTargets();
    }

    GroupProperties(GroupProperties theCopy) {
        super(theCopy);  // takes care of the ID.
        groupName = theCopy.groupName;
        groupType = theCopy.groupType;
        linkTargets = (LinkTargets) theCopy.linkTargets.clone();
    } // end of the copy constructor


    // A specific type copy constructor cannot be called from a reference;
    //   this method can be.  Child classes will override it so that a calling
    //   context does not need to know what generation of GroupProperties it is
    //   really getting, just that it will behave like a new GroupProperties.
    protected GroupProperties copy() {
        return new GroupProperties(this);
    }


    GroupInfo getGroupInfo() {
        return new GroupInfo(this);
    }


    String getGroupName() {
        return groupName;
    }

    // Used by 'saveAs' and other one-off situations.
    // We do not setGroupChanged() at this level; the calling context should do that, if needed.
    void setGroupName(String theName) {
        groupName = theName;
    }

}
