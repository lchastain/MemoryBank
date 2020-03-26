import com.fasterxml.jackson.core.type.TypeReference;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Vector;

@SuppressWarnings({"unchecked", "rawtypes"})
public class NoteDataVector implements Iterable<Object> {
    Vector theVector;

    public NoteDataVector() {
        theVector = new Vector(0, 1);
    }

    public NoteDataVector(Object theData) {
        NoteData.loading = true; // We don't want to affect the lastModDates!
        theVector = AppUtil.mapper.convertValue(theData, new TypeReference<Vector<NoteData>>() {  });
        NoteData.loading = false; // Restore normal lastModDate updating.
    }

    @NotNull
    @Override
    public Iterator<Object> iterator() {
        return theVector.iterator();
    }

//    void add(Object theItem) {
//        theVector.add(theItem);
//    }
//
//    int size() {
//        return theVector.size();
//    }

    Vector<NoteData> getVector() {
        return theVector;
    }

}
