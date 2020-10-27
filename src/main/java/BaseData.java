import java.time.ZonedDateTime;
import java.util.UUID;

public class BaseData {
    static boolean loading = false;
    protected String zdtLastModString;
    protected UUID instanceId;

    BaseData() {
        instanceId = UUID.randomUUID();
        if(!loading) {  // This mechanism may NOT be needed after all.
            // Yes, jackson runs this constructor, but THEN it overlays zdtLastModString with the value that it parsed out.
            zdtLastModString = ZonedDateTime.now().toString();
        }
    }

//    @Override
//    public boolean equals(Object theOtherOne) {
//        // Breakpoint until we find who calls / needs this - IJ gives >1000 results
//        if (getClass() != theOtherOne.getClass()) return false;
//        return instanceId.toString().equals(((BaseData) theOtherOne).instanceId.toString());
//    }

//    // This is used during uniqueness checking.  This method effectively disables
//    // the 'hashcode' part of the check, so that the only remaining uniqueness criteria
//    // is the result of the .equals() method.
//    @Override
//    public int hashCode() {
//        return 1;
//    }


}
