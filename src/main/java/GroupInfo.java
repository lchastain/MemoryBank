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

    // NoteGroupPanel extends NoteGroupFile which extends NoteGroup, of which this class is one part, so by
    // defining a NoteGroupPanel here we are essentially adding a grandchild to our class definition.  But that sets
    // us up for an infinite recursion loop during serialization, so we keep this reference in a transient member.
    // That way it does not ever get serialized, but the downside is that when that data is deserialized into an
    // instance of this class, that instance will not have a value for 'myNoteGroupPanel'.  Then we populate the
    // transient member with the grandchild if/when it comes into scope.
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
        myNoteGroupPanel = theCopy.myNoteGroupPanel;
        simpleName = theCopy.simpleName;
    }

    String getCategory() {
        String theCategory = groupType.toString();
        if(theCategory.endsWith(" Note")) theCategory = GroupType.NOTES.toString();
        return theCategory;
    }


    // There is one use case for this method - auto-removing a reverse link.  Called from LinkagesEditorPanel.
    NoteGroupDataAccessor getNoteGroupDataAccessor() {
        // A GroupInfo instance can be a component of several
        // different possible constructs, but the most common source of this class when this method is called will be
        // that it comes from the targetGroupInfo of a LinkedEntityData which has been reconstructed by deserializing
        // serialized data.  In that case its 'myNoteGroupPanel' will be null.  But that does not mean that we should
        // just load up a new group for it; the indicated group could still be currently loaded into a Panel that is in
        // one of the main app's keepers.  If it is, then
        // THAT group's GroupInfo WILL have a properly set myNoteGroupPanel, but we are not that GroupInfo; we are the
        // one that was deserialized (two separate GroupInfo instances can still be for the same group).  So - if our
        // group is referenced by a panel in a keeper then we want to get that panel, because it might have unsaved
        // changes for the group, that we should adopt (and preserve) before returning it from here.
        //
        // Alternative/converse statement of essentially the same point:
        // There is no operationally-correct way that 'myNoteGroupPanel' would be not-null and yet the panel it refers
        //   to would not be held in a keeper.  AppTreePanel is the only keeper-manager, and it keeps a panel as soon
        //   as it is constructed, and panel construction is when the transient member is set.  LinkTargetSelectionPanel
        //   will only make a new panel if that panel is not already in a keeper, and it does not add that new panel to
        //   a keeper; the reference to it is thrown away when link selection concludes.
        // It is true that we could check myNoteGroupPanel and if found to be not-null, call refresh on it to ensure
        //   that it is updated, and return that.  But when it IS null our recourse is to check for the presence of
        //   the group in a keeper, if so then call refresh on it, and return that.  Between the two there is an
        //   overlap of actions but the second case actually covers both, since as it was stated earlier, if
        //   myNoteGroupPanel is not null then that panel WILL be in a keeper, but if it IS null then it still
        //   MIGHT be in a keeper.
        //
        // For all these reasons, in this method we don't actually care about myNoteGroupPanel; we will only get our
        //   answer either from a keeper or load it from persisted data.

        // Get the group's panel from a keeper, if it is there -
        NoteGroupPanel thePanel = AppTreePanel.theInstance.getNoteGroupFromKeeper(groupType, groupName);
        if(thePanel != null) {
            thePanel.refresh(); // Preserve any unsaved changes.
            // No need to remove from keeper after this; any link target changes that we make next will pass through
            // and still take effect there.
            // but this is another point to VERIFY - TODO
            return thePanel;
        }

        // So if we arrive here it means that our group is not referenced by a currently active panel but it must have
        // been in a panel at some point in the past and it was preserved at that time, so we will use the preserved
        // data to create a new group, and return that.  If the load fails for any reason, the return value will be null.
        return NoteGroupFactory.loadNoteGroup(this);
    }

    // Get the NoteGroupPanel that was made from this GroupInfo (but there may not be one in current memory).
    //
    // This method is used by the LinkagesEditorPanel, to get the in-memory reference to a previously constructed
    // panel so that it can be used to save or remove a reverse link in that target group panel.  If the panel is
    // not in current memory, a null is returned and the calling context can search for the panel in
    // persisted data, if needed.
    NoteGroupPanel getNoteGroupPanel() {
        NoteGroupPanel thePanel;

// old -----------
        // This particular instance may have been created by a NoteGroupPanel constructor.
        // In that case the 'answer' is already known.
        if(myNoteGroupPanel != null) thePanel = myNoteGroupPanel;
        else { // But otherwise we need to ask around -
            // The only other available access to in-memory panels is via the AppTreePanel instance.
            thePanel = AppTreePanel.theInstance.getNoteGroupFromKeeper(groupType, groupName);

        }
// old -----------


// new -----------
        // First we need to try to retrieve the panel from its keeper, if it is there.
        thePanel = AppTreePanel.theInstance.getNoteGroupFromKeeper(groupType, groupName);

        if(thePanel == null) {
            // This particular instance may have been created by a NoteGroupPanel constructor.
            // In that case the 'answer' is already known.  Otherwise it came from serialized
            // data and
            // HOWEVER - don't see how the one from the keeper could be null and this one not null.
            // keep a breakpoint here until proven one way or the other.
            if(myNoteGroupPanel != null) {
                thePanel = myNoteGroupPanel;
            }
        }
// new -----------

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
        return groupName;
    }


    // With this member being set in the constructor, it looks like this method would never be needed,
    // but this class existed in various forms over time, and data was persisted before the group name
    // was added to it.  So when that data comes back in now to the current class definition, groupName could be
    // missing from the file data, in which case this class would be reconstructed without it and that
    // is when this method can be used to fix that.
    void setGroupName(String theName) { groupName = theName; }

}
