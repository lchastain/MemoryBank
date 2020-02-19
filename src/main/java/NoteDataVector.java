import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

public class NoteDataVector extends DataVector implements Iterable<Object> {

    public NoteDataVector(Object theData) {
        NoteData.loading = true; // We don't want to affect the lastModDates!
        theVector = AppUtil.mapper.convertValue(theData, new TypeReference<Vector<NoteData>>() {  });
        NoteData.loading = false; // Restore normal lastModDate updating.
    }

//    void add(Object theItem) {
//        theVector.add(theItem);
//    }
//
//    int size() {
//        return theVector.size();
//    }

}
