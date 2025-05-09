import com.fasterxml.jackson.annotation.JsonIgnore;

// This class holds the Search parameters.
// After reading this:  https://dzone.com/articles/getter-setter-use-or-not-use-0
//   decided to NOT use getters/setters here, and that encapsulation for this
//   data class is not appropriate.  It is package-private, not intended for
//   access from anywhere other than the SearchPanel and SearchResultGroup
//   (and Tests).
//-----------------------------------------------------------------------
class SearchPanelSettings  {
    // Keyword Configuration
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

    // Where to look; defaulting to true
    boolean typeGoal = true;
    boolean typeDay = true;
    boolean typeMonth = true;
    boolean typeYear = true;
    boolean typeOtherNote = true;
    boolean typeFutureEvent = true;
    boolean typeTask = true;

    @JsonIgnore  // Remove this after all current 'Lee' searches have been removed/redone without it.
    // Optional - hand-edit the .json files.
    @SuppressWarnings("unused")
    boolean typePastEvent = true;


    // Note Dates
    int whenChoice; // Before / Between / After
    String noteDateWhen1String;
    String noteDateWhen2String;

    // Last Mod Dates
    int modChoice; // Before / Between / After
    String dateLastMod1String;
    String dateLastMod2String;

    // A 'Search Name' is not a data member here, so we
    //   do not want to use a getter to retrieve it; Jackson
    //   would make the wrong assumption about the Class.
    String determineSearchResultsName() {
        StringBuilder sb = new StringBuilder();
        if(AppUtil.isPopulated(word1)) {
            if (not1) sb.append("NOT ");
            sb.append(word1);

            if (AppUtil.isPopulated(word2)) {
                if (or1) sb.append(" OR ");
                else if (and1) sb.append(" AND ");
                if (not2) sb.append("NOT ");
                sb.append(word2);
            }
        }
        else sb.append(NoteGroupFile.getTimestamp());

        return sb.toString();
    }

} // end SearchPanelSettings
