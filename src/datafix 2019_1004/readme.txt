This is a one-time-use module that will be used to:
    1.  Change 4 Dates to String, in SearchResultsGroupProperties.SearchPanelSettings
            dateWhen1 --> noteDateWhen1String
            dateWhen2 --> noteDateWhen2String
            dateMod1  --> dateLastMod1String
            dateMod2  --> dateLastMod2String
        For these, do NOT need to get the old values and put them into the new Strings, because
        all currently saved searches prior to this change were keyword-based, only.  So all
        that is needed is to drop the Dates and add the Strings - this will be done automatically
        along with just doing a load, cast to the right Vector, and then a save.

    2.  Remove the DayNoteData.noteDateString
        With the last DataFix, this member was added in the thought that it could be used
        in place of getting the date from the path/filename.  But further design analysis
        determined that this was a bad, non-normalized practice; the same data element
        would be repeated on every note in a datafile.  So - its use was dropped and it
        was marked to be ignored.  However it is still in the data and until no file has
        it, it needs to stay in the class (but ignored).  So this fix will get it out of
        the data so that it can be removed from the class altogether.

Two runs were needed here also, but only to change a var name, from sps --> searchPanelSettings

Minor code changes between the two runs; one in DataFix to set the flag, and adding a @JsonIgnore
in the classes.

File types affected by this fix are DayNotes and SearchResults, only.

While in development and in use, the source code here will be marked as a source directory for the project.
Afterwards, that designation will be removed and this module should be kept indefinitely as a copy source
for future DataFix versions.

(use the Project Structure to enable/disable source code).
