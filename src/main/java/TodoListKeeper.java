import java.util.Vector;

/**
 Basically a wrapper for a vector of TodoNoteGroups (TodoLists).  The idea is
 that since loading of a list can sometimes take long enough for the user to
 notice/care, we could maybe just load them once, and when not currently
 selected, keep them out of sight but in memory and ready to quickly redisplay.
 Using a vector vs an ArrayList, for additional speed and less overhead; we do
 not need any of the 'Collections' bells and whistles (such as sorting).
 The idea does work (fairly well, actually) but it turns out that there are
 some operations (RENAME, SAVE-AS) that after they complete, it would be better
 to remove the copy here so that upon reselection, the list will be reloaded.
 For those operations we provide the 'remove' method, and every new list action
 should consider whether or not to use it.

 */

public class TodoListKeeper {
    Vector<TodoNoteGroup> theLists;

    public TodoListKeeper() {
        theLists = new Vector<TodoNoteGroup>();
    }

    public void add(TodoNoteGroup tng) { theLists.add(tng); }

    public TodoNoteGroup get(String aListName) {
        // Search the TodoLeaf Vector for the list.
        for (TodoNoteGroup tng : theLists) {
            String tngName = TodoNoteGroup.prettyName(tng.getGroupFilename());
            if (aListName.equals(tngName)) {
                return tng;
            } // end if
        } // end for
        return null;
    }

    // Scan the vector looking for the indicated list and if found, remove.
    //----------------------------------------------------------------
    public void remove(String aListName) {
        TodoNoteGroup theGroup = null; // Keep a temporary reference

        // Search the TodoLeaf Vector for the list.
        for (TodoNoteGroup tng : theLists) {
            String tngName = TodoNoteGroup.prettyName(tng.getGroupFilename());
            if (aListName.equals(tngName)) {
                theGroup = tng;
                // Note: cannot remove from within this loop;
                // ConcurrentModificationException.
                break;
            } // end if
        } // end for

        // If found, then remove.  Otherwise no action needed.
        if (theGroup != null) {
            MemoryBank.debug("  Removing " + aListName + " from the TodoListKeeper");
            theLists.removeElement(theGroup);
        } // end if
    } // end remove

} // end class TodoListKeeper
