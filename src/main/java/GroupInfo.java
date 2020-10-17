// This class holds the persistent metadata for a Group.
// In addition to the ID and LastModDate that it gets from BaseData, it holds
//   a simple group name (String) and the type of group (enum).

import java.util.Random;

class GroupInfo extends BaseData {
    static Random random = new Random();

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
    private String groupName; // The name of the group, as shown in the Tree.
    private String simpleName; // The name of the group, as shown in the Tree.  See more in comments in getGroupName

    public GroupInfo() {} // Jackson uses this when loading json string text into instances of this class.

    GroupInfo(String theName, GroupType theType) {
        super();
        groupName = theName;
        groupType = theType;
    }

    // When this copy constructor is called with child classes of GroupInfo, the
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
        groupName = theCopy.groupName;
        groupType = theCopy.groupType;
        simpleName = theCopy.simpleName;
    }

    String getCategory() {
        String theCategory = groupType.toString();
        if(theCategory.endsWith(" Note")) theCategory = GroupType.NOTES.toString();
        return theCategory;
    }


    // Called from LinkagesEditorPanel when adding or removing a reverse link.
    NoteGroupDataAccessor getNoteGroupDataAccessor() {
        // Get the group's panel from a keeper, if it is there -
        NoteGroupPanel thePanel = AppTreePanel.theInstance.getPanelFromKeeper(groupType, groupName);
        if(thePanel != null) {
            thePanel.refresh(); // Preserve any unsaved changes.
            // No need to remove from keeper after this; any link target changes that we make next will pass through
            // and still take effect there.
            // but this is another point to VERIFY - TODO
            return thePanel;
        }

        // So if we arrive here it means that our group is not referenced by a currently active panel but it must have
        // been in a panel at some point in the past and it was preserved at that time, so we will use the preserved
        // data to create a new panel, and return that.  If the load fails for any reason, the return value will be null.
        return GroupPanelFactory.loadNoteGroupPanel(this);
    }

    // The condition in the method is needed during a transitional member name change (simpleName --> groupName).
    // TODO - run a data fix (or something) to do replacement on all pre-existing data, then remove simpleName
    //  from this class and simplify this method.
    // Find the problems by:  grep -r simpleName * | grep -v null
    String getGroupName() {
        if(simpleName != null) {
            groupName = simpleName;
            simpleName = null;
        }
        return groupName;
    }


    // With this member being set in the constructor, it looks like this method would never be needed,
    // but this class existed in various forms over time, and data was persisted before the group name
    // was added to it.  So when that data comes back in now to the current class definition, groupName could be
    // missing from the file data, in which case this class would be reconstructed without it and that
    // is when this method can be used to fix that.
    void setGroupName(String theName) { groupName = theName; }

}
