import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

public class SearchResultVector extends NoteDataVector {

    public SearchResultVector(Object theData) {
        NoteData.loading = true; // We don't want to affect the lastModDates!
        theVector = AppUtil.mapper.convertValue(theData, new TypeReference<Vector<SearchResultData>>() {  });
        NoteData.loading = false; // Restore normal lastModDate updating.
    }

}
