import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

public class TodoDataVector extends DataVector {

    public TodoDataVector(Object theData) {
        NoteData.loading = true; // We don't want to affect the lastModDates!
        theVector = AppUtil.mapper.convertValue(theData, new TypeReference<Vector<TodoNoteData>>() {  });
        NoteData.loading = false; // Restore normal lastModDate updating.
    }

}
