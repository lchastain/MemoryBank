import java.io.File;
import java.io.Serializable;
import java.util.Date;

public class SearchResultData extends NoteData implements Serializable {
    static final long serialVersionUID = -1257203205705118689L;

    // The 'dateNoteWhen' member is used to hold the date 'choice' when
    //   handling a click on a button in the 'Found In' column, prior
    //   to changing to a calendar-based view.  Of course, not all
    //   sources are calendar-based, and those that are not, do not
    //   need this member to handle the click.  However, it is also
    //   used as the sort key when sorting by 'Found In', since the
    //   textual dates and file names that are displayed would not
    //   necessarily sort in the expected order.
    //   For Events, the 'dateEventStart' (if available) is kept here.
    //   For Todo items, the 'dateTodoItem' (if available) is kept.
    //   When sorting on 'Found In', if this date is still null then
    //   the sort treats it as a 'no key' and places it ????
    private Date dateNoteWhen;

    private File fileFoundIn;

    // Called during a search - the other members will be set
    //   explicitly, in subsequent method calls.
    public SearchResultData(NoteData nd) {
        super(nd);
    } // end constructor


    // called by swap - need to set all data members now.
    public SearchResultData(SearchResultData srd) {
        super(srd);
        dateNoteWhen = srd.getNoteDate();
        fileFoundIn = srd.getFileFoundIn();
    } // end constructor


    public File getFileFoundIn() {
        return fileFoundIn;
    }

    protected Date getNoteDate() {
        return dateNoteWhen;
    }

    //-----------------------------------------------------
    // Method Name: getFoundIn
    //
    // Returns a short, easily readable string indicating which file
    //   this result originally comes from.  For calendar-based
    //   sources, decided to use alpha months rather than numeric
    //   because it "reads" best and does not affect sorting
    //   which uses the 'dateNoteWhen'.
    //-----------------------------------------------------
    public String getFoundIn() {
        String retstr; // RETurn STRing

        String fname = fileFoundIn.getName();
        String fpath = fileFoundIn.getParent();

        retstr = fname; // as a default; will probably change, below.

        if (fname.endsWith(".todolist")) {
            retstr = fname.substring(0, fname.lastIndexOf('.'));
        } else if (fname.equals("UpcomingEvents")) {
            retstr = "Upcoming";
        } else if (!fpath.endsWith("MemoryBank")) {
            // If the path does not end at the top level data
            //   directory, then (at least at this writing) it
            //   means that we are down one of the calendar-
            //   based 'Year' paths.
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

    public void setFileFoundIn(File f) {
        fileFoundIn = f;
    }

    public void setNoteDate(Date value) {
        dateNoteWhen = value;
    }

} // end class SearchResultData
