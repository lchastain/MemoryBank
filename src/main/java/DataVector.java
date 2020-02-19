import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Vector;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class DataVector implements Iterable<Object> {
    Vector theVector;

    public DataVector() {
        theVector = new Vector(0, 1);
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
