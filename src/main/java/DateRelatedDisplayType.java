// Every category of data in the application will have a uniquely named
//   DataArea that will be an indicator of how to access the data.
// The name will mean different things to different accessor types -
//   for instance, the name will help a file-based accessor to develop
//   a path and filename to the data, whereas a database type accessor
//   will use the same name to determine which db(s) and table(s) to use.
// The String version of the name may be used when presenting choices
//   for certain user actions.

enum DateRelatedDisplayType {
    DAY_NOTES("Day Notes"),
    MONTH_NOTES("Month Notes"),
    YEAR_NOTES("Year Notes"),
    MONTH_VIEW("Month View"),
    YEAR_VIEW("Year View");

    private final String nodeName;

    DateRelatedDisplayType(String s) {
        nodeName = s;
    }

    @Override
    public String toString() {
        return nodeName;
    }
}
