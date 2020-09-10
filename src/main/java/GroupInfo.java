// This class holds the persistent metadata for a Group.
// In addition to the ID and LastModDate that it gets from BaseData, it holds
//   a simple group name (String) and the type of group (enum).

import java.util.Random;

// This class is encapsulated into the properties of a NoteGroupPanel and when a NoteGroupPanel is constructed,
// the reference to that panel is stored in the transient member 'myNoteGroupPanel'.  However this class can also
// be constructed separately, either directly or reconstructed from serialized data, in which case myNoteGroupPanel
// is initially null but in all valid use cases an associated panel will exist or will have existed at one time.
// See more on this in the comments to the getNoteGroupPanel method.

class GroupInfo extends BaseData {
    static Random random = new Random();
    transient NoteGroupPanel myNoteGroupPanel;

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
    private String simpleName; // The name of the group, as shown in the Tree.

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
        myNoteGroupPanel = theCopy.myNoteGroupPanel;
        simpleName = theCopy.simpleName;
    }

    String getCategory() {
        String theCategory = groupType.toString();
        if(theCategory.endsWith(" Note")) theCategory = GroupType.NOTES.toString();
        return theCategory;
    }


    // Get the NoteGroupPanel that was made from this GroupInfo (but there may not be one in current memory).
    // NoteGroupPanel extends NoteGroupFile which extends NoteGroupData, of which this class is one part, so
    // essentially we are asking for a grandchild, but adding a grandchild to our class definition would have set
    // us up for an infinite recursion loop during serialization, so we keep this reference in a transient member.
    // That way it does not ever get serialized, but the downside is that when the data is deserialized into an
    // instance of this class, that instance will not have a value for 'myNoteGroupPanel'.  So in that case we can
    // populate the member with the grandchild (if it has come into scope) when this method is called.
    //
    // This method is used by the LinkagesEditorPanel, to get the in-memory reference to a previously constructed
    // panel so that it can be used to save or remove a reverse link in that target group panel.  If the panel is
    // not in current memory, a null is returned and the calling context can search for the panel in
    // persisted data, if needed.
    NoteGroupPanel getNoteGroupPanel() {
        NoteGroupPanel thePanel;

        // This particular instance may have been created by a NoteGroupPanel constructor.
        // In that case the 'answer' is already known.
        if(myNoteGroupPanel != null) thePanel = myNoteGroupPanel;
        else { // But otherwise we need to ask around -
            // The only other available access to in-memory panels is via the AppTreePanel instance.
            thePanel = AppTreePanel.theInstance.getNoteGroupFromKeeper(groupType, groupName);

        }

        return thePanel;
    }

    // The condition in the method is needed during a transitional member name change (simpleName --> groupName).
    // TODO - run a data fix (or something) to do replacement on all pre-existing data, then remove simpleName
    //  from this class and simplify this method.
    String getGroupName() {
        if(simpleName != null) {
            groupName = simpleName;
            simpleName = null;
        }
        return groupName; }

    // With this member being set in the constructor, it looks like this method would never be needed,
    // but this class existed in various forms over time, and data was persisted before the group name
    // was added to it.  So when that data comes back in now to the current class definition, groupName could be
    // missing from the file data, in which case this class would be reconstructed without it and that
    // is when this method can be used to fix that.
    void setGroupName(String theName) { groupName = theName; }

}
