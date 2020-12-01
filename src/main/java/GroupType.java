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


    @Override
    public String toString() {
        return display;
    }
}
