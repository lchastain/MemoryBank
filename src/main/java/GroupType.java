enum GroupType {
    NOTES("Notes"),
    CALENDAR_NOTES("Calendar Notes"),
    DAY_NOTES("Day Note"),      // Non-plural here is correct; needed when group naming.
    MONTH_NOTES("Month Note"),  // Non-plural here is correct; needed when group naming.
    YEAR_NOTES("Year Note"),    // Non-plural here is correct; needed when group naming.
    GOALS("Goal"),
    GOAL_LOG("Goal Log"),
    GOAL_NOTES("Goal Notes"),
    GOAL_TODO("Goal To Do"),
    LOG("Log"),
    MILESTONE("Milestone"),
    EVENTS("Event"),
    TODO_LIST("To Do List"),
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
