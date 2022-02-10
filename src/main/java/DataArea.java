// Every category of data in the application will have a uniquely named
//   DataArea that will be an indicator of how to access the data.
// The name will mean different things to different accessor types -
//   for instance, the name will help a file-based accessor to develop
//   a path and filename to the data, whereas a database type accessor
//   will use the same name to determine which db(s) and table(s) to use.
// The String version of the name may be used when presenting choices
//   for certain user actions.

enum DataArea {
    ARCHIVES("Archives"),
    CALENDARS("Years"),
    GOALS("Goals"),
    GOAL_LOG("Goals"),
    UPCOMING_EVENTS("Upcoming Events"),
    MILESTONES("Goals"),
    NOTES("Notes"),
    ICONS("icons"),
    APP_ICONS("icons"),
    IMAGES("images"),
    LOGS("Logs"),
    TODO_LISTS("To Do Lists"),
    TODO_LOG("To Do Lists"),
    SEARCH_RESULTS("Search Results");

    private final String areaName;

    DataArea(String s) {
        areaName = s;
    }

    static DataArea getAreaFromGroupType(GroupType groupType) {
        DataArea theArea = null;
        switch(groupType) {
            case GOALS:
                theArea = DataArea.GOALS;
                break;
            case TODO_LIST:
                theArea = DataArea.TODO_LISTS;
                break;
            case EVENTS:
                theArea = DataArea.UPCOMING_EVENTS;
                break;
            case SEARCH_RESULTS:
                theArea = DataArea.SEARCH_RESULTS;
                break;
        }
        return theArea;
    }

    // This is the name formatted for data storage (no spaces)
    String getAreaName() {
        return areaName.replaceAll("\\s", "");
    }

    // This is the pretty-printed version of the area name.
    @Override
    public String toString() {
        return areaName;
    }
}
