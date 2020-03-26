import java.time.LocalDate;

// This class holds the persistent data for the SearchResultGroup
//-----------------------------------------------------------------------
public class SearchResultGroupProperties {

    SearchPanelSettings searchPanelSettings;
    String searchDateString; // This is needed, now that a search can be renamed.  No way to 'see' it, yet.

    public String column1Label;
    public String column2Label;
    public String column3Label;
    public String column4Label;

    public int columnOrder;

    public SearchResultGroupProperties() {
        searchDateString = LocalDate.now().toString();
        column1Label = "Found in";
        column2Label = "Note Text";
        column3Label = "Last Modified";
        column4Label = "";  // placeholder
        columnOrder = 123;  // 1234
    } // end constructor

    void setSearchSettings(SearchPanelSettings s) {
        searchPanelSettings = s;
    } // end setSearchSettings

} // end SearchResultGroupProperties
