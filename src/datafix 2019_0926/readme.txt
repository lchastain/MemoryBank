This is a one-time-use module that will be used to add a TimeZone to the last mod date of every note (of mine) that
has ever been archived.  It will also change the name of one field in TodoItems, and for Calendar-based notes it
will retain the original date and time but will add the timezone where I was living at the time of the Note.  The
TimeZone assignment for last mod uses that same logic.

It will replace the java.util.Date lastModDate of every NoteData with a String that is generated from a
java.time.ZonedDateTime - 'zonedDateTimeLastMod' (it will be a String, since Jackson encounters an infinite
recursion when trying to jsonify a ZonedDateTime).  Since the original lastModDate has been found to be worthless,
it does not need to be examined as part of the process and the fix will set date-based Notes to their corresponding
dates, and any others will unfortunately just get the current date.

For the date-based notes there are additional wrinkles - going to ZonedDateTime, we need to set the TimeZone.  I
laid out timelines for the zones I was in for dates ranging since 1978, but when making a ZDT for the notes where
a non-default TZ is applied, it adjusts the time portion of the original date, effectively corrupting it.  But we have
to have the right TZ for the new ZDT, AND we want to have the right time.  So how to proceed?
1.  Get the correct date by making one from the full path/filename.
2.  Get the correct time from the timeOfDayDate member of the NoteData by using a modified 'makeTimeString' method.
    (The time changes depending on the timezone; eventually learned that I could make and use the correct zones
    without ever having to change the default TimeZone of the JVM).  (the learing part of that was long and frustrating)
3.  Get the correct TimeZone by maing a ZonedDateTime out of the Date that came from the Filename.  Using that Date
    is important, because it will be factored in when the conversion determines which offset to apply to the TimeZone;
    sometimes Austin could be -6, others it is -5.  Parse the String of the new ZDT to get that TimeZone portion.
    Ex:  "2018-01-02T00:00:00.382+04:00[Europe/Samara]" -->      +04:00[Europe/Samara]
4.  Reassemble a correct String: 2019-07-18T08:57+04:00[Europe/Samara]
    (If seconds and milliseconds are zeros, they are not shown by 'toString' and not needed when parsing)
5.  Parse the new string back in to a new ZonedDateTime, use it in a call to the new setNoteDateTime method.
This is what is done for DayNoteData; Month and Year notes have different handling in that they aren't concerned with
the time portion and have no additional 'timeOfDay' member.  So for them it's only about the LMD, but without the
two-stage String construction, adding the correct timezones would affect the date back one and
since these default to the 00:00 on the first day of the Month/Year, Jan 01 of a given year would be pulled back into
Dec 31 of the previous year, for example.  That's not acceptable even given the accepted unreliability of the LMDs;
there will be Search Results showing that, and we don't want LMDs being earlier than Found Ins.


While in development and in use, the source code here will be marked as a source directory for the project.
Afterwards, that designation will be removed and this module should be kept indefinitely as a copy source
for future DataFix versions.

There are two main scenarios where a fix would be needed - the simpler one involves adding or removing data members
in one or more NoteGroup types.  The other case is where the data storage and retrieval mechanism itself is changing.
Here we are doing the first case, so the standard load and save methods are used, but copied to here and given a new
util class name because this solution would break if we used the ones in the main app and then they later changed.
And there are other data classes to consider as well - SearchResultData, TodoNoteData, ... ?

Maybe these DataFix apps should each be in their own project or module?  Probably, but it's pretty convenient to have
them right here with the main app, especially as I continue to turn up and fix new problems in the app as I go.

For this effort only, a temporary new method is added to NoteData.java:
    public void setBadBadThing(String newLastModDateString) {
        zdtLastModString = newLastModDateString;
    }

It should be gone from there before the next DataFix is needed.

(use the Project Structure to enable/disable source code).
