This is a one-time-use module that will be used to convert the NoteGroup data of the main app from serialized
objects to JSON data.

While in development and in use, the source code here will be marked as a source directory for the project.
Afterwards, that designation will be removed and this module should be kept indefinitely as a copy source
for future DataFix versions.

This version of a DataFix has evolved from earlier versions where a scan of all data files was done but then the
SearchPanel settings were consulted to narrow the results to fit search parameters.  The 'fixit' mechanism was also
there in the Search code, embedded but disabled after use via commenting-out.  Now that the fix capability is needed
yet again, it makes more sense to separate the two activities and prepare this one (somewhat) for reusability as well
as acknowledging its one-time usage while at the same time cleaning up the Search code.

The 'somewhat' reusability mentioned above - doesn't seem to fit with a single usage module, so some elaboration
will help: the reuse will come AFTER its one-time usage, from standing as an archived cloning source when the NEXT data
fix is needed.  At that time, clone this one and then modify the copy as needed to fit the requirements of the new fix.
Keep both copies but only enable the newer one.  This is the preferred method because there is no way to write a fixit
generic enough to support any standard reuse, yet some features such as the directory scanning may not need to change,
and while a new one is in development it will help to have a complete, unchanged copy of one that had been working.

There are specific file exceptions for each time the 'fix' is run.  For example the current effort is to
take all data files from serialized objects to JSON content.  But some files are already converted, so any
file ending in .json is omitted, as well as the 'icons' directory, and others.  Which ones are to be
excluded each time cannot be predicted; they will have to be accounted for by code changes within each DataFix.

There are two main scenarios where a fix would be needed - the simpler one involves adding or removing data members
in one or more NoteGroup types.  The other case is where the data storage and retrieval mechanism itself is changing.
For this latter case (the one we're writing now) the original load method is needed, so that is copied here and used
to perform the fix.  Shortly afterwards it will be gone from the main code, and this would be the only version of
it remaining (other than in 'git', of course).  Next time a different fix is needed, it will also be necessary for it
to utilize the updated load method.

(use the Project Structure to enable/disable source code).
