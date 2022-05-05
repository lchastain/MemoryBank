// This class holds the persistent metadata for a Group.
// In addition to the ID and LastModDate that it gets from BaseData, it holds
//   a simple group name (String) and the type of group (enum).

import java.time.LocalDate;
import java.util.UUID;

// A GroupInfo is primarily just a Type and a Name.
// From that you can get a full NoteGroup or a NoteGroupPanel.

class GroupInfo {

    UUID groupId; // The ID of the group that this info references.  Can be null when not needed.
    GroupType groupType;     // Says what kind of group it is.  Values defined above.
    private String groupName; // The name of the group, as shown in the Tree.
    transient String archiveName; // The human-readable one, with colons in the time portion (or null).

    public GroupInfo() { }


    GroupInfo(String theName, GroupType theType) {
        groupName = theName;
        groupType = theType;
        archiveName = null;
    }

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
        NoteGroup theNoteGroup = null;
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
                case GOAL_TODO:
                    theNoteGroup = new TodoNoteGroup(this);
                    break;
                case EVENTS:
                    theNoteGroup = new EventNoteGroup(this);
                    break;
                case LOG:  // No longer planned to be standalone.  Remove this option when this comment gets too old.  5 May 2022
                case GOAL_LOG:
                    theNoteGroup = new LogNoteGroup(this);
                    break;
                case GOALS:
                    theNoteGroup = new GoalGroup(this);
                    break;
                case MILESTONE:
                    theNoteGroup = new MilestoneNoteGroup(this);
                    break;
                case NOTES:
                case GOAL_NOTES:
                    theNoteGroup = new PlainNoteGroup(this);
                    break;
                case DAY_NOTES:
                    theNoteGroup = new DayNoteGroup(this);
                    break;
                case MONTH_NOTES:
                case YEAR_NOTES:
                    theNoteGroup = new CalendarNoteGroup(this);
            }
        }

        // If we have a groupId then we only want theNoteGroup if the ID matches.
        // If the ID does not match then return null but if we don't have an ID
        // to match with then we can return the loaded or new NoteGroup.
        if(theNoteGroup != null && groupId != null) {
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
