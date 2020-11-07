import java.io.File;

// This class is used to preserve Search results.

public class SearchResultData extends NoteData {
    private File fileFoundIn;
    GroupInfo foundIn;  // Not (yet?) using a setter for this...

    // The JSON mapper uses this one during a load; IntelliJ doesn't find a usage.
    public SearchResultData() {
        super();
    } // end default constructor


    // Called during a search - foundIn will be set
    //   explicitly, directly.
    public SearchResultData(NoteData nd) {
        super(nd);
    } // end constructor


    // called by swap - need to set all data members now.
    public SearchResultData(SearchResultData srd) {
        super(srd);
        fileFoundIn = srd.getFileFoundIn();
        foundIn = srd.foundIn;
    } // end constructor


    File getFileFoundIn() {
        return fileFoundIn;
    }

    GroupInfo getFoundIn() {
        return foundIn;
    }

    void setFileFoundIn(File f) {
        fileFoundIn = f;
        if(getMyNoteGroup() != null) {
            getMyNoteGroup().setGroupChanged(true);
        }

        // The LMD of a SearchResult is (currently) of no concern.
        // This 'set' method will not address it.
    }

} // end class SearchResultData
