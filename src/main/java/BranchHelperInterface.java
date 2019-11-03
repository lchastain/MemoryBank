import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

public interface BranchHelperInterface {
    StringBuilder ems = new StringBuilder();  // Error Message String
    int MAX_FILENAME_LENGTH = 32; // Arbitrary, but helps with UI issues.

    // Called by the TreeBranchEditor to see if there is any objection to a rename
    // of a node from or to the provided value.  If true, the rename goes thru. If
    // false, it simply discards the rename action.  If any user feedback is desired,
    // Your implementation can provide that before returning the boolean.
    default boolean allowRenameFrom(DefaultMutableTreeNode theNode) {
        return true;
    }

    default boolean allowRenameTo(String theNewName) {
        return true;
    }

    //-------------------------------------------------------------------
    // Method Name:  checkFilename
    //
    // Check theProposedName for the following 'illegal' file naming conditions:
    //   No entry, or whitespace only.
    //   The name ends in '.json'.
    //   The name contains more than MAX_FILENAME_LENGTH chars.
    //   The filesystem refuses to create a file with this name.
    //
    // Return Value - A 'complaint' string if name is not valid,
    //    otherwise an empty string.
    //
    // static rather than default due to external references, but all of
    // those could easily be changed, except for TodoNoteGroup.saveAs;
    // and moving it to here breaks too many other method calls from
    // within that one to local methods in the TodoNoteGroup instance.
    // Of course there could be solutions to that too, but how far down
    // that road do we want to go, for what added value?  Don't know
    // about the value so how far is 'not at all' (for now); this works.
    // But now - what about moving this one to NoteGroup?  think about it.
    //-------------------------------------------------------------------
    static String checkFilename(String theProposedName, String basePath) {
        ems.setLength(0);

        String testName = theProposedName.trim();

        // Check for non-entry (or evaporation).
        if (testName.isEmpty()) ems.append("No New List Name was supplied!");

        // Refuse unwanted help.
        if (testName.endsWith(".json")) {
            ems.append("The name you supply cannot end in '.json'");
        } // end if

        // Check for legal max length.
        // The input field used in this class would not allow this one, but
        //   since this method is static and accepts input from outside
        //   sources, this check should be done.
        if (testName.length() > MAX_FILENAME_LENGTH) {
            ems.append("The new name is limited to " + MAX_FILENAME_LENGTH + " characters");
        } // end if

        // If there have been any problems found up to this point then we don't want to
        // continue checking, so return the answer now.
        if (!ems.toString().isEmpty()) return ems.toString();

        // Do we want to check to see if a file with this name already exists?
        //   No.  The handler for the renaming event will disallow that particular
        //   situation, so we don't need to repeat that logic here.

        // Do we want to check to see if a file with this name already exists?
        //   No.  The handler for the renaming event will disallow that particular
        //   situation, so we don't need to repeat that logic here.  Likewise for
        //   the 'Save As' logic.
        //
        //   As for the 'Add New List' functionality that is also a consumer of this
        //   method, the AppTreePanel handles that situation by simply opening it, a
        //   kind of back-door selection of the existing list.


        // Note - I thought it would be a good idea to check for 'illegal'
        //   characters in the filename, but when I started testing, the
        //   Windows OS accepted %, -, $, ! and &; not sure what IS illegal.
        //   Of course there ARE illegal characters, and the ones above may
        //   still be illegal on another OS.  So, the best way to
        //   detect them is to try to create a file using the name we're
        //   checking.  Any io error and we can fail this check.

        // Now try to create the file, with an additional '.test' extension.  This will not
        // conflict with any 'legal' existing file in the directory, so if there is any
        // problem at all then we know it's a bad name.
        String theFilename = basePath + testName + ".test";
        File f = new File(theFilename);
        MemoryBank.debug("Name checking new file: " + f.getAbsolutePath());
        // This existence is not the same check as the one that we said would not be done; different extension.
        boolean b = f.exists();
        try { // If the File operations below generate any exceptions, it's a bad name.
            // This first part & check are just to ensure that we do the rest of the test with
            // a 'clean' slate and don't have 'leftovers' from some earlier attempt(s).
            if (b) {
                b = f.delete(); // This MIGHT throw an exception.
                if(!b && f.exists()) {
                    // It shouldn't have existed in the first place, and now it
                    // refuses to be deleted.  This is enough cause for concern
                    // to justify disallowing the new name.  No Exception was
                    // thrown in this case but we will complain about it anyway -
                    ems.append("A previous test file refuses to be deleted.");
                }
            } else {
                b = f.createNewFile();
                if(!b || !f.exists()) {
                    // This probably would not happen since any name that does
                    // not cause an Exception should result in a created file.
                    // But it's less risky to just check for and handle it anyway.
                    ems.append("Unable to create a test file with the proposed name");
                } else {
                    // Ok, so we just now created the test file, but did it really get the requested name?
                    // This is not such an outlandish question - a filename with a colon in it CAN be created
                    // but the Windows OS will just drop the colon and anything after it.  The createNewFile()
                    // method appears to be much more permissive than the renameTo(), so we will now test the
                    // rename as well, and we don't even have to try a different name.
                    b = f.renameTo(new File(theFilename));
                    if(!b) { // No Exception thrown, but no joy with the rename operation, either.
                        ems.append("The proposed new name was rejected by the operating system!");
                    }

                    // Ok, so it was renamed to itself.  Now delete it, and verify that this was done as well.
                    b = f.delete();
                    if(!b || f.exists()) {
                        ems.append("Unable to remove the test file with the proposed new name!");
                    }
                }
            }
        } catch (IOException | SecurityException e) {
            ems.append(e.getMessage());
        } // end try/catch

        return ems.toString();
    } // end checkFilename

    // Called by the TreeBranchEditor to determine whether or not to provide a 'Delete'
    // button for each of the choices.  If false then no choice will have a Delete button.
    default boolean deletesAllowed() {
        return true;
    }

    // The textual list of choices for all items that the user might select for
    // inclusion in the final Branch.  Usually includes everything that is already
    // in the branch to edit, and any available but still unselected choices.  If
    // your implementation returns a null, the editor will default to using the
    // nodes of the provided branch.
    ArrayList<String> getChoices();


    // This method will return the node with the specified name.  It does a breadth-first
    // search and returns the first one that it finds, so if a user has named a deeper
    // node the same as the one we're searching for it will not interfere with the one
    // that was requested.  If for some reason a lower one is desired then make the call
    // with 'theRoot' referencing a deeper branch rather than the root of the full tree.
    @SuppressWarnings("rawtypes")
    static DefaultMutableTreeNode getNodeByName(DefaultMutableTreeNode theRoot, String theName) {
        DefaultMutableTreeNode dmtn = null;
        Enumeration bfe = theRoot.breadthFirstEnumeration();

        while (bfe.hasMoreElements()) {
            dmtn = (DefaultMutableTreeNode) bfe.nextElement();
            if (dmtn.toString().equals(theName)) {
                break;
            }
        }
        return dmtn;
    }

    // Called by the TreeBranchEditor to determine whether or not to allow a drop of
    // one leaf onto another, thereby making the drop target into a 'parent'.  If
    // false then the drop is not allowed.  Drops 'between' leaves for the purposes
    // of reordering the nodes of the branch are always allowed, as are drops onto
    // nodes that are already a parent.
    default boolean makeParents() {
        return false;
    }

    // The handler for the 'Apply' button.
    void doApply(MutableTreeNode mtn, ArrayList<NodeChange> changes);

    // What text appears on the 'Remove' button.  Ex:  'Delete', 'Remove', or
    // something else.  If your implementation returns a null, the editor will
    // use a default of 'X'.
    default String getDeleteCommand() {
        return null;
    }
}
