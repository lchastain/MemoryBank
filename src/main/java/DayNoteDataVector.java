import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

public class DayNoteDataVector extends DataVector {

    public DayNoteDataVector(Object theData) {
        NoteData.loading = true; // We don't want to affect the lastModDates!
        theVector = AppUtil.mapper.convertValue(theData, new TypeReference<Vector<DayNoteData>>() {  });
        NoteData.loading = false; // Restore normal lastModDate updating.
    }


}
