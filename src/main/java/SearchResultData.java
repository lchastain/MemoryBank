// This class is used to preserve Search results.

public class SearchResultData extends NoteData {
    GroupInfo foundIn;  // Not (yet?) using a setter for this...

    // The JSON mapper uses this one during a load; IntelliJ doesn't find a usage.
    public SearchResultData() { } // end default constructor


    // Called during a search - foundIn will be set
    //   explicitly, directly.
    public SearchResultData(NoteData nd) {
        super(nd);
    } // end constructor


    // called by swap - need to set all data members now.
    public SearchResultData(SearchResultData srd) {
        super(srd);
        foundIn = srd.foundIn;
    } // end constructor


    GroupInfo getFoundIn() {
        return foundIn;
    }

} // end class SearchResultData
