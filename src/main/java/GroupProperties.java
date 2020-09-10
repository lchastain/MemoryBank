// This class holds the persistent metadata for a Group.
// In addition to the ID and LastModDate that it gets from BaseData, it holds
//   a simple group name (String) and the type of group (enum).

public class GroupProperties extends GroupInfo {

    LinkTargets linkTargets;

    // This constructor is only used by Tests or by Jackson type conversion operations.
    GroupProperties() {
        this("No Name Yet", GroupType.UNKNOWN);
    }

    GroupProperties(String theName, GroupType theType) {
        super(theName, theType);
        linkTargets = new LinkTargets();
    }

    GroupProperties(GroupProperties theCopy) {
        super(theCopy);
        linkTargets = (LinkTargets) theCopy.linkTargets.clone();
    } // end of the copy constructor


    // A specific type copy constructor cannot be called from a reference;
    //   this method can be.  Child classes will override it so that a calling
    //   context does not need to know what generation of GroupProperties it is
    //   really getting, just that it will behave like a new GroupProperties.
    protected GroupProperties copy() {
        return new GroupProperties(this);
    }

}
