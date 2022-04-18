enum GroupType {

    NOTES("Note"),
    CALENDAR_NOTES("Calendar Notes"),
    DAY_NOTES("Day Note"),
    MONTH_NOTES("Month Note"),
    YEAR_NOTES("Year Note"),
    GOALS("Goal"),
    GOAL_LOG("Goal Log"),
    GOAL_TODO("Goal To Do"),
    LOG("Log"),
    MILESTONE("Milestone"),
    EVENTS("Event"),
    TODO_LIST("To Do List"),
    TODO_LOG("To Do Log"),
    SEARCH_RESULTS("Search Result"),
    UNKNOWN("Unknown");

    private final String display;

    GroupType(String s) {
        display = s;
    }


    @Override
    public String toString() {
        return display;
    }
}
