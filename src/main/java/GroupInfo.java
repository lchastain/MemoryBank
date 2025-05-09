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
            theNoteGroup = switch (groupType) {
                case SEARCH_RESULTS -> new SearchResultGroup(this);
                case  TODO_LIST, GOAL_TODO -> new TodoNoteGroup(this);
                case EVENTS -> new EventNoteGroup(this);  // No longer planned to be standalone.  Remove this option when this comment gets too old.  5 May 2022
                case LOG, GOAL_LOG -> new LogNoteGroup(this);
                case GOALS -> new GoalGroup(this);
                case MILESTONE -> new MilestoneNoteGroup(this);
                case NOTES, DAY_NOTES, GOAL_NOTES -> new DayNoteGroup(this);
                case MONTH_NOTES, YEAR_NOTES -> new CalendarNoteGroup(this);
                default -> theNoteGroup;
            };
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
            thePanel.preClosePanel(); // Ensures in-progress changes are persisted prior to a reload.
        } else { // There isn't a pre-existing Panel for it, so we will just make one now.
            LocalDate theDate; // Needed by the Calendar note types.
            switch (groupType) {
                case SEARCH_RESULTS -> thePanel = new SearchResultGroupPanel(groupName);
                case TODO_LIST -> thePanel = new TodoNoteGroupPanel(groupName);
                case EVENTS -> thePanel = new EventNoteGroupPanel(groupName);
                case GOALS -> thePanel = new GoalGroupPanel(groupName);
                case NOTES -> thePanel = new DateTimeNoteGroupPanel(groupName);
                case DAY_NOTES -> {
                    thePanel = new DayNoteGroupPanel();
                    theDate = CalendarNoteGroup.getDateFromGroupName(this);
                    ((DayNoteGroupPanel) thePanel).setDate(theDate);
                }
                case MONTH_NOTES -> {
                    thePanel = new MonthNoteGroupPanel();
                    theDate = CalendarNoteGroup.getDateFromGroupName(this);
                    ((MonthNoteGroupPanel) thePanel).setDate(theDate);
                }
                case YEAR_NOTES -> {
                    thePanel = new YearNoteGroupPanel();
                    theDate = CalendarNoteGroup.getDateFromGroupName(this);
                    ((YearNoteGroupPanel) thePanel).setDate(theDate);
                }
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
