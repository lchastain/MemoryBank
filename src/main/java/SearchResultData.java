import java.beans.Transient;
import java.io.File;

// This class is used to preserve Search results.

public class SearchResultData extends NoteData {
    private File fileFoundIn;

    // The JSON mapper uses this one during a load; IntelliJ doesn't find a usage.
    public SearchResultData() {
        super();
    } // end default constructor

    // Called during a search - the other members will be set
    //   explicitly, in subsequent method calls.
    public SearchResultData(NoteData nd) {
        super(nd);
    } // end constructor


    // called by swap - need to set all data members now.
    public SearchResultData(SearchResultData srd) {
        super(srd);
        fileFoundIn = srd.getFileFoundIn();
    } // end constructor


    File getFileFoundIn() {
        return fileFoundIn;
    }

    //-----------------------------------------------------
    // Method Name: getFoundIn
    //
    // Returns a short, easily readable string indicating which file
    //   this result originally comes from.  Used in the display of
    //   that info, not for file loading or prettyNaming.  For calendar-based
    //   sources, decided to use alpha months rather than numeric
    //   because it "reads" best and does not affect sorting.

    // BUT THAT SHOULD CHANGE - go to 'yyyy-mm-dd' or filename, so that sorting would be 'in order'.

    //   Had to annotate as Transient, else the JSON mapper picks this
    //   up and runs it when saving a file.  Then the loader doesn't
    //   recognize it as a class member, and raises an Exception.
    //-----------------------------------------------------
    @Transient
    String getFoundIn() {
        String retstr; // RETurn STRing

        String fname = fileFoundIn.getName();
        String fpath = fileFoundIn.getParent();

        retstr = fname; // as a default; will probably change, below.

        if (fname.startsWith("todo_")) {
            retstr = fname.substring(5, fname.lastIndexOf('.'));
//      } else if (fname.endsWith(".todolist")) { // Older data; remove when it's all gone.
//          retstr = fname.substring(0, fname.lastIndexOf('.'));
        } else if (fname.startsWith("event_")) {
            retstr = fname.substring(6, fname.lastIndexOf('.'));
        } else {
            // If the name hasn't already been recognized then it means that
            //   we are (should be) down one of the calendar-based 'Year' paths.
            String strYear = fpath.substring(fpath.lastIndexOf(File.separatorChar) + 1);

            if (fname.startsWith("Y")) {
                retstr = strYear;
            } else {
                // We get the numeric Month from character
                //   positions 1-2 in the filename.
                String strMonthInt = fname.substring(1, 3);
                int intMonth = Integer.parseInt(strMonthInt);

                String[] monthNames = new String[]{"Jan", "Feb",
                        "Mar", "Apr", "May", "Jun", "Jul",
                        "Aug", "Sep", "Oct", "Nov", "Dec"};

                String strMonth = monthNames[intMonth - 1];

                if (fname.startsWith("M")) {
                    retstr = strMonth + " " + strYear;
                } else if (fname.startsWith("D")) {
                    // We get the numeric Day from character
                    //   positions 3-4 in the filename.
                    String strDay = fname.substring(3, 5);
                    retstr = strDay + " " + strMonth + " ";
                    retstr += strYear;
                } // end if
            } // end if
        } // end if

        return retstr;
    } // end getFoundIn

    void setFileFoundIn(File f) {
        fileFoundIn = f;
        // The LMD of a SearchResult is (currently) of no concern.
        // This 'set' method will not address it.
    }

} // end class SearchResultData
