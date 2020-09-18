

public interface NoteGroupDataAccessor {
    enum AccessResult {
        UNNECESSARY,
        SUCCESS,
        FAILURE
    }


    // Saving is an operation that happens often, sometimes per user
    //   action and sometimes automatically.
    //   TODO - we should be able to tell the difference and act accordingly.
    //
    //      maybe with another param to this method.  not all operations
    //      come directly here; there is also the preClose code, and
    //      that one also could be auto or user directed.
    //
    //   (Obviously, some of this commenting belongs in the implementation code and not the interface declaration;
    //    will move it after that code settles down and the lines of separation become more clear).
    //
    //  Currently -
    //   If errors are encountered, this method can trap and print
    //   them to the screen but it will not halt execution or
    //   attempt interaction with the user.  A status variable is
    //   set at various points; calling contexts that 'care' about the
    //   results should check it and handle the values according to
    //   those situations.
    AccessResult saveNoteGroupData();

}
