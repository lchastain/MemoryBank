// This class holds the persistent metadata for a Group.
// In addition to the ID and LastModDate that it gets from BaseData, it holds
//   a simple group name (String) and the type of group (enum).

import java.util.Random;

public class GroupProperties extends BaseData {
    private String simpleName; // The short (pretty) name of the group, as shown in the Tree.
    static Random random = new Random();

    enum GroupType {
        NOTES("Note"),
        GOALS("Goal"),
        EVENTS("Event"),
        TODO_LIST("To Do List"),
        SEARCH_RESULTS("Search Result"),
        UNKNOWN("Unknown");

        private final String display;

        GroupType(String s) {
            display = s;
        }

        // Used in dev/test
        public static GroupType getRandomType() {
            return values()[random.nextInt(values().length)];
        }

        @Override
        public String toString() {
            return display;
        }
    }
    GroupType groupType;     // Says what kind of group this is.  Values defined above.


    // Used by the Jackson code during load / conversion of the list to a Vector.
    GroupProperties() {
        super();
        simpleName = "No Name Yet";
        groupType = GroupType.UNKNOWN;
    }

    // When this copy constructor is called with child classes of GroupProperties,
    // the effect is to strip off their extra baggage.  A simple upcast gives you
    // the right class but does not remove the unwanted members.  After this, the
    // result will be serialized with only the base data, and it cannot be cast back
    // to its original type.
    GroupProperties(GroupProperties theCopy) {
        super();
        instanceId = theCopy.instanceId;
        zdtLastModString = theCopy.zdtLastModString;
        simpleName = theCopy.simpleName;
        groupType = theCopy.groupType;
    }

    GroupProperties(String theName, GroupType theType) {
        super();
        simpleName = theName;
        groupType = theType;
    }

    String getName() { return simpleName; }

}
