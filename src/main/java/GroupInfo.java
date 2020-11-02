// This class holds the persistent metadata for a Group.
// In addition to the ID and LastModDate that it gets from BaseData, it holds
//   a simple group name (String) and the type of group (enum).

import java.util.Random;

// A GroupInfo is primarily just a Type and a Name.
// From that you can get a full NoteGroup or a NoteGroupPanel or a NoteGroupDataAccessor.

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

    public GroupInfo() {
    } // Jackson uses this when loading json string text into instances of this class.

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

    // The return value will just be the string representation of the Type, unless it is a
    // Calendar note type in which case it will just be 'Note'.
    String getCategory() {
        String theCategory = groupType.toString();
        if (theCategory.endsWith(" Note")) theCategory = GroupType.NOTES.toString();
        return theCategory;
    }

    // Get the one unique NoteGroup that goes with this GroupInfo.
    // The NoteGroup will either come from a pre-constructed Panel, or we will make a new one.
    NoteGroup getNoteGroup() {
        NoteGroup theNoteGroup;
        // Try to get the NoteGroup from an existing Panel
        NoteGroupPanel thePanel = null;
        if (AppTreePanel.theInstance != null) {
            // This condition is only here for tests; under normal operating conditions
            //   theInstance of AppTreePanel would never be null.
            thePanel = AppTreePanel.theInstance.getPanelFromKeeper(this);
        }

        if (thePanel != null) { // It worked!
            theNoteGroup = thePanel.myNoteGroup;
            thePanel.preClosePanel(); // Ensures persisted data matches Panel data.
        } else { // There isn't a Panel for it, so we will just make a NoteGroup of the right type; Panel not needed.
            switch (groupType) {
                case SEARCH_RESULTS:
                    theNoteGroup = new SearchResultGroup(this);
                    break;
                case TODO_LIST:
                    theNoteGroup = new TodoNoteGroup(this);
                    break;
                case EVENTS:
                    theNoteGroup = new EventNoteGroup(this);
                    break;
                case GOALS:
                    theNoteGroup = new GoalGroup(this);
                    break;
                case DAY_NOTES:
                    theNoteGroup = new DayNoteGroup(this);
                    break;
                case MONTH_NOTES:
                case YEAR_NOTES:
                default:
                    theNoteGroup = new NoteGroup(this);
            }
        }
        return theNoteGroup;
    }

    // Not much logic here at the moment, but coming soon - there will be other choices as to what
    // other accessors may be returned.  Then - will need to have a way to switch between them.
    NoteGroupDataAccessor getNoteGroupDataAccessor() {
        return new NoteGroupFile(this);
    }

    // Called from LinkagesEditorPanel when adding or removing a reverse link.
//    NoteGroupPanel getNoteGroupDataAccessor() {
//        // Get the group's panel from a keeper, if it is there -
//        NoteGroupPanel thePanel = AppTreePanel.theInstance.getPanelFromKeeper(groupType, groupName);
//        if(thePanel != null) {
//            thePanel.refresh(); // Preserve any unsaved changes.
//            // No need to remove from keeper after this; any link target changes that we make next will pass through
//            // and still take effect there.
//            return thePanel;
//        }
//
//        // So if we arrive here it means that our group is not referenced by a currently active panel but it must have
//        // been in a panel at some point in the past and it was preserved at that time, so we will use the preserved
//        // data to create a new panel, and return that.  If the load fails for any reason, the return value will be null.
//        return GroupPanelFactory.loadNoteGroupPanel(this);
//    }

    // The condition in the method is needed during a transitional member name change (simpleName --> groupName).
    // TODO - run a data fix (or something) to do replacement on all pre-existing data, then remove simpleName
    //  from this class and simplify this method.
    // Find the problems by:  grep -r simpleName * | grep -v null
    String getGroupName() {
        if (simpleName != null) {
            groupName = simpleName;
            simpleName = null;
        }
        return groupName;
    }


    // Used by 'saveAs' and other one-off situations.
    void setGroupName(String theName) {
        groupName = theName;
    }

}
