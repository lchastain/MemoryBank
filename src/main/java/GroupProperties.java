// This class holds the persistent metadata for a Group.
// In addition to the ID and LastModDate that it gets from BaseData, it holds
//   a simple group name (String) and the type of group (enum).

import java.util.Vector;

public class GroupProperties extends GroupInfo {

    Vector<LinkedEntityData> linkTargets;

    GroupProperties() {
        this("No Name Yet", GroupType.UNKNOWN);
    }

    GroupProperties(String theName, GroupType theType) {
        super(theName, theType);
        linkTargets = new Vector<>(0, 1);
    }

    @SuppressWarnings("unchecked")
    GroupProperties(GroupProperties theCopy) {
        super(theCopy);
        this.linkTargets = (Vector<LinkedEntityData>) theCopy.linkTargets.clone();
    } // end of the copy constructor


    // A copy constructor cannot be called from a reference;
    //   this method can be.  Child classes will override it so that a calling
    //   context does not need to know what generation of GroupProperties it is
    //   really getting, just that it will look like a GroupProperties.
    protected GroupProperties copy() {
        return new GroupProperties(this);
    }

}
