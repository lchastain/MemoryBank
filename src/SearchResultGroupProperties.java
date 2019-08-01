import java.io.Serializable;

// This class holds the persistent data for the SearchResultGroup
//-----------------------------------------------------------------------
public class SearchResultGroupProperties implements Serializable {
    public static final long serialVersionUID = 2412760123507069513L;

    public SearchPanelSettings sps;

    public String column1Label;
    public String column2Label;
    public String column3Label;
    public String column4Label;

    public int columnOrder;

    public SearchResultGroupProperties() {
        column1Label = "Found in";
        column2Label = "Note Text";
        column3Label = "Last Modified";
        column4Label = "";  // placeholder
        columnOrder = 123;  // 1234
    } // end constructor

    public void setSearchSettings(SearchPanelSettings s) {
        sps = s;
    } // end setSearchSettings

} // end SearchResultGroupProperties
