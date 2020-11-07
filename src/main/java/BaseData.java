import java.time.ZonedDateTime;
import java.util.UUID;

public class BaseData {
    // The base constructor here is unfortunately used by Jackson during loads and type conversions, and
    // during that construction is when the zdtLastModString (LMD) is set, even when we are reconstructing data that
    // hasn't actually changed.  So we use the 'loading' boolean to avoid unwanted updates to the LMD.
    //
    // Also, some children have additional members that when changed can & should update the LMD, but those 'set'
    // methods are also called during the construction of a Group while it is loading in pre-existing data.
    // Here again, we need to avoid the update to the LMD in that circumstance and so the context that loads the
    // group will first have to set 'loading' to true, and then put it back to 'false' when done.
    static boolean loading = false;

    protected String zdtLastModString;
    protected UUID instanceId;

    BaseData() { // This constructor is (unfortunately) used by Jackson during loads and type conversions.
        instanceId = UUID.randomUUID();
        zdtLastModString = ZonedDateTime.now().toString();
    }

    void touchLastMod() {
        if(!loading) zdtLastModString = ZonedDateTime.now().toString();
    }

    @Override
    public String toString() {
        return AppUtil.toJsonString(this);
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
