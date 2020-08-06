// This class holds the persistent metadata for a Group.
// In addition to the ID and LastModDate that it gets from BaseData, it holds
//   a simple group name (String) and the type of group (enum).

import java.util.Random;

public class GroupInfo extends BaseData {
    static Random random = new Random();
    transient NoteGroup myNoteGroup; // This is assigned during link target selection (only, for now?)

    enum GroupType {
        NOTES("Note"),
        DAY_NOTES("Day Note"),
        MONTH_NOTES("Month Note"),
        YEAR_NOTES("Year Note"),
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
    private final String simpleName; // The short (pretty) name of the group, as shown in the Tree.

    public GroupInfo() {
        this("No Name Yet", GroupType.UNKNOWN);
    }

    GroupInfo(String theName, GroupType theType) {
        super();
        simpleName = theName;
        groupType = theType;
    }

    // When this copy constructor is called with child classes of GroupProperties, the
    // effect is to strip off their extra baggage.  A simple upcast would give you
    // the right class but does not remove the unwanted members, which cause conversion
    // problems when deserializing.  After this, the result will be serialized with only
    // the base class data, and it cannot be cast back to its original child type.
    // This particular constructor is used by LinkedEntityData to populate its member
    //   'targetGroupInfo' when a new link is created, after having chosen a link target.
    GroupInfo(GroupInfo theCopy) {
        super();
        instanceId = theCopy.instanceId;
        zdtLastModString = theCopy.zdtLastModString;
        simpleName = theCopy.simpleName;
        groupType = theCopy.groupType;
        myNoteGroup = theCopy.myNoteGroup;
    }

    String getCategory() {
        String theCategory = groupType.toString();
        if(theCategory.endsWith(" Note")) theCategory = GroupType.NOTES.toString();
        return theCategory;
    }


    // Get the NoteGroup that this GroupInfo belongs to.
//     It should have been set <somehow, that it probably wasn't>
    NoteGroup getGroup() {
        return myNoteGroup;
    }

    String getName() { return simpleName; }

}
