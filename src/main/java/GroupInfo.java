// This class holds the persistent metadata for a Group.
// In addition to the ID and LastModDate that it gets from BaseData, it holds
//   a simple group name (String) and the type of group (enum).

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.util.UUID;

// A GroupInfo is primarily just a Type and a Name.
// From that you can get a full NoteGroup or a NoteGroupPanel.

class GroupInfo {

    UUID groupId; // The ID of the group that this info references
    GroupType groupType;     // Says what kind of group it is.  Values defined above.
    private String groupName; // The name of the group, as shown in the Tree.
    transient String archiveName; // The human-readable one, with colons in the time portion (or null).

    // These members were previously defined here or inherited, but not now and so we need to acknowledge
    //   that we might see them in previously persisted data, but 'ignore' them otherwise.
    //   This is so that they do not gum up the type conversions when this object gets deserialized.
    //--------------------------------------------------
    @JsonIgnore
    protected String zdtLastModString;

    @JsonIgnore
    protected UUID instanceId;

    @JsonIgnore
    protected String simpleName; // A previous version of 'groupName'.  Needs to be removed from all data.
    //--------------------------------------------------

    public GroupInfo() { }


    GroupInfo(String theName, GroupType theType) {
        groupName = theName;
        groupType = theType;
        archiveName = null;
    }

    // This particular constructor is used by LinkedEntityData to populate its member
    //   'targetGroupInfo' when a new link is created, after having chosen a link target.
    GroupInfo(GroupProperties groupProperties) {
        groupId = groupProperties.instanceId;
        groupName = groupProperties.getGroupName();
        groupType = groupProperties.groupType;
    }


    // Get the one unique NoteGroup that goes with this GroupInfo.
    // If no match to the ID then return null but if we ourselves have
    // no ID then return a NoteGroup of the requested type, whether we
    // can find one (first preference) or cannot, so must make one.
    NoteGroup getNoteGroup() {
        NoteGroup theNoteGroup;
        // Try to get the NoteGroup from an existing Panel
        NoteGroupPanel thePanel = null;
        if (AppTreePanel.theInstance != null && archiveName == null) {
            // This condition is only here for tests; under normal operating conditions
            //   theInstance of AppTreePanel would never be null.
            thePanel = AppTreePanel.theInstance.getPanelFromKeeper(this);
        }

        if (thePanel != null) { // The NoteGroup was already instantiated in a Panel.
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
                case LOG:
                case GOAL_LOG:
                case TODO_LOG:
                    theNoteGroup = new LogGroup(this);
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

        // If we have a groupId then we only want theNoteGroup if the ID matches.
        // If the ID does not match then return null but if we don't have an ID
        // then we can return the loaded or new NoteGroup.
        if(groupId != null) {
            if(!groupId.toString().equals(theNoteGroup.getGroupProperties().instanceId.toString())) {
                theNoteGroup = null;
            }
        }
        return theNoteGroup;
    }

    // This method will either find and return the one unique existing Panel for this GroupInfo, or it will
    // make one and return that.  If it has to make one, it will not add it to a keeper.
    NoteGroupPanel getNoteGroupPanel() {
        // Try to get the NoteGroup from an existing Panel
        NoteGroupPanel thePanel = null;
        if (AppTreePanel.theInstance != null && archiveName == null) {
            // This condition is only here for tests; under normal operating conditions
            //   theInstance of AppTreePanel would never be null.
            thePanel = AppTreePanel.theInstance.getPanelFromKeeper(this);
        }

        if (thePanel != null) { // It worked!
            thePanel.preClosePanel(); // Ensures persisted data matches Panel data.
        } else { // There isn't a Panel for it, so we will just make a NoteGroup of the right type; Panel not needed.
            LocalDate theDate; // Needed by the Calendar note types.
            switch (groupType) {
                case SEARCH_RESULTS:
                    thePanel = new SearchResultGroupPanel(groupName);
                    break;
                case TODO_LIST:
                    thePanel = new TodoNoteGroupPanel(groupName);
                    break;
                case EVENTS:
                    thePanel = new EventNoteGroupPanel(groupName);
                    break;
                case GOALS:
                    thePanel = new GoalGroupPanel(groupName);
                    break;
                case DAY_NOTES:
                    thePanel = new DayNoteGroupPanel();
                    theDate = CalendarNoteGroup.getDateFromGroupName(this);
                    ((DayNoteGroupPanel) thePanel).setDate(theDate);
                    break;
                case MONTH_NOTES:
                    thePanel = new MonthNoteGroupPanel();
                    theDate = CalendarNoteGroup.getDateFromGroupName(this);
                    ((MonthNoteGroupPanel) thePanel).setDate(theDate);
                    break;
                case YEAR_NOTES:
                    thePanel = new YearNoteGroupPanel();
                    theDate = CalendarNoteGroup.getDateFromGroupName(this);
                    ((YearNoteGroupPanel) thePanel).setDate(theDate);
                    break;
            }
        }
        return thePanel;
    }


    boolean exists() {
        NoteGroupDataAccessor noteGroupDataAccessor = MemoryBank.dataAccessor.getNoteGroupDataAccessor(this);
        return noteGroupDataAccessor.exists();
    }


    String getArchiveStorageName() {
        if(archiveName == null) return null;
        DataAccessor dataAccessor = MemoryBank.dataAccessor;
        return dataAccessor.getArchiveStorageName(archiveName);
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
