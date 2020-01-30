// This class holds the Search parameters.
// After reading this:  https://dzone.com/articles/getter-setter-use-or-not-use-0
//   decided to NOT use getters/setters here, and that encapsulation for this
//   data class is not appropriate.  It is package-private, not intended for
//   access from anywhere other than the SearchPanel and SearchResultGroup
//   (and Tests).
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

    int whenChoice;
    String noteDateWhen1String;
    String noteDateWhen2String;

    int modChoice;
    String dateLastMod1String;
    String dateLastMod2String;

} // end SearchPanelSettings
