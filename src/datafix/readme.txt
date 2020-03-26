This is a one-time-use module that will be used to:

    1.  Remove from BaseData:
            noteId - replaced with 'instanceId'; more global, not tied to 'notes'.

    2.  Remove from NoteData:
            linkages - moved from being a list of LinkData on individual noteDatas to an overall single global list of
                a new class (LinkedNoteData) in appOptions.  LinkedNoteData has its own list of another new class -
                LinkTarget.

    3.  Create the GroupNames list - this obviates the need to give every NoteGroup a properties class.
            (temporarily, at least)

And a manual fix of moving all the 'year' data under a 'Years' directory in the user data area.

Some now-obsolete production classes have been copied to this area so that the fixit MIGHT continue to work and be
  usable after its one-time use.  But there are other 'production' classes involved; no guarantee that this code
  will continue to work after the codebase has moved on.

How to remove a newly json-ignored data member from a class (IF you don't need any values from it):
   1.  After it has been ignored, need to load and then re-save the data.  It will not appear in the saved data,
        regardless of whether or not it was there for the load.
   2.  Remove the ignored member from the class.  No future data loads will trip over it, IF you have fixed
        all data repos.

While in development and in use, the source code here will be marked as a source directory for the project.
Afterwards, that designation will be removed and this module should be kept indefinitely as a copy source
for future DataFix versions.

(use the Project Structure to enable/disable source code).
