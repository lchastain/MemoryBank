This is a one-time-use module that will be used to:

    1.  Remove from NoteData:
            extendedNoteWidthInt
            extendedNoteHeightInt
        Previously just json-ignored but as long as there's this datafix anyway, time to
        completely remove.

    2.  Restore to NoteData:
            the ID - as a UUID
        This was there for a long long time, hoping to be used in linkages.  Finally it was removed
        after IJ complained too much about the unused var, but now - it's going to be used.

    3.  Add to NoteData:
            ArrayList<Linkage> linkages;
        A list of links to other items.  Also adding the Linkage.java class

    4.  Remove from TodoNoteData:
            private String strLinkage;
                and also the 'linkage' that was made by Jackson converter, for the get/set of strLinkage.
        Because the list of linkages in NoteData will replace this.  They are json-ignored during this run,
            then can be removed altogether.

FixUtil is just the AppUtil that was being used to load/save, at the time this data fix was needed.  It was
copied to here so that this code might be used again if needed, even if AppUtil evolves.  A rename was
necessary in order to avoid a name collision with the one still in production code (that is otherwise still
identical to the one here but that can only certain for the current usage of this datafix).

But there are still other 'production' classes involved; no guarantee that this code will continue to work
correctly after its one-time use and the codebase has moved on.

All 'datafix' accesses to FixUtil are static.

How to remove a newly json-ignored data member from a class (IF you don't need any values from it):
   1.  After it has been ignored, need to load and then re-save the data.  It will not appear in the saved data,
        regardless of whether or not it was there for the load.
   2.  Remove the ignored member from the class.  No future data loads will trip over it, IF you have fixed
        all data repos.

While in development and in use, the source code here will be marked as a source directory for the project.
Afterwards, that designation will be removed and this module should be kept indefinitely as a copy source
for future DataFix versions.

(use the Project Structure to enable/disable source code).
