import java.time.LocalDate;
import java.util.Date;

// This class holds the Search parameters.
//-----------------------------------------------------------------------
class SearchPanelSettings  {
    boolean not1;
    boolean not2;
    boolean not3;
    String word1;
    String word2;
    String word3;
    boolean and1;
    boolean and2;
    boolean or1;
    boolean or2;
    boolean paren1;
    boolean paren2;

    // After a DataFix, mark all Date type as JsonIgnore
    Date dateWhen1;
    Date dateWhen2;
    private String noteDateWhen1String;
    private String noteDateWhen2String;
    int whenChoice;

    Date dateMod1;
    Date dateMod2;
    private String dateLastMod1String;
    private String dateLastMod2String;
    int modChoice;

    SearchPanelSettings() {
    }

    LocalDate getNoteDateWhen1() {
        return LocalDate.parse(noteDateWhen1String);
    }

    LocalDate getNoteDateWhen2() {
        return LocalDate.parse(noteDateWhen2String);
    }

    LocalDate getDateLastMod1() {
        return LocalDate.parse(dateLastMod1String);
    }

    LocalDate getDateLastMod2() {
        return LocalDate.parse(dateLastMod2String);
    }

    void setNoteDateWhen1(LocalDate value) {
        if(value == null) return;
        noteDateWhen1String = value.toString();
    }

    void setNoteDateWhen2(LocalDate value) {
        if(value == null) return;
        noteDateWhen2String = value.toString();
    }

    void setDateLastMod1(LocalDate value) {
        if(value == null) return;
        dateLastMod1String = value.toString();
    }

    void setDateLastMod2(LocalDate value) {
        if(value == null) return;
        dateLastMod2String = value.toString();
    }

} // end SearchPanelSettings
