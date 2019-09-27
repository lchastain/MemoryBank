import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

// The code for scanDataDir was brought over from the last DataFix.
// (and modified as needed for use here, although the main difference is
// the change from running a search function, to calling the 'fixTheFile'
// method).   Some (but not all)
// associated classes were also copied over to here unchanged except
// they were given new names (ex: AppUtil --> FixUtil),
// to preserve this working in the case that they are modified in
// the main app, in the future.


public class DataFix {

    // create time zone objects in the DEFAULT time zone, before it gets changed, below.
    TimeZone theDefaultZone = TimeZone.getDefault(); // SAMT, currently.
    TimeZone tzone0 = TimeZone.getTimeZone("America/Phoenix"); // Sierra Vista, Tucson AZ
    TimeZone tzone1 = TimeZone.getTimeZone("America/Los_Angeles"); // San Diego, Lawndale CA
    TimeZone tzone2 = TimeZone.getTimeZone("America/Chicago"); // Austin TX
    TimeZone tzone3 = theDefaultZone; // Izhevsk, Russia

    // Yes, using deprecated constructor for this one-shot operation.  This whole class will go away after the 'fix'.
    Date earlyAZ = new Date(1979 - 1900, 2, 10);
    Date firstCA = new Date(1992 - 1900, 3, 18);
    Date back2AZ = new Date(2004 - 1900, 8, 26);
    Date back2CA = new Date(2008 - 1900, 4, 7);
    Date austin = new Date(2019 - 1900, 6, 14);


    private DataFix() {
    }

    // This method finds data files.  It does not load them, but it sends
    // the files that it finds to 'fixTheFile', which does load.
    private void scanDataDir(File theDir, int level) {
        MemoryBank.dbg("Scanning " + theDir.getAbsolutePath());

        File[] theFiles = theDir.listFiles();
        assert theFiles != null;
        int howmany = theFiles.length;
        MemoryBank.debug("\t\tFound " + howmany + " items");

        for (File aFile : theFiles) {
            String aFileName = aFile.getName();
            if (aFile.isDirectory()) {
                if (aFileName.equals("Archives")) continue;
                if (aFileName.equals("icons")) continue;
                scanDataDir(aFile, level + 1);
            } else {
                // The list below needs to be a complete list of any files that
                // we don't want to look at; these 'other' type data files
                // will not load properly.  There are other ways to handle this
                // situation but this is what was chosen - it works well enough.
                if (aFileName.endsWith(".dump")) continue;
                if (aFileName.equals("appOpts.json")) continue;
                if (aFileName.endsWith(".txt")) continue;
                if (aFileName.equals("DaySubjects")) continue;
                if (aFileName.equals("DayNoteDefaults")) continue;
                if (aFileName.equals("EventNoteDefaults")) continue;
                if (aFileName.equals("UpcomingSubjects")) continue;
                if (aFileName.equals("UpcomingLocations")) continue;
                if (aFileName.equals("YearSubjects")) continue;
                if (aFileName.equals("MonthSubjects")) continue;
                if (aFileName.startsWith("Export")) continue;
                fixTheFile(aFile);
            } // end if
        }//end for i
    } //end scanDataDir

    // A modification of the searchDataFile method, from AppTreePanel
    private void fixTheFile(File dataFile) {
        String theFilename = dataFile.getName();
        String theAbsolutePath = dataFile.getAbsolutePath();
        System.out.println("Fixing: " + dataFile.getAbsolutePath());
        Vector<NoteData> noteDataVector = new Vector<>();

        // Load the data from the file.
        Object[] theGroupData = FixUtil.loadNoteGroupData(dataFile);
        if (theGroupData != null && theGroupData[theGroupData.length - 1] != null) {

            // Get the data into a 'recognizable' Vector type.  Then we can work with it.
            // (but some of the fixing is being done right now, by loading it into classes that inherit from the NoteData
            // class (that has been modified since the data was first stored).  Deserializing it into the new class
            // structure will get us started with the needed changes).
            if (theFilename.equals("UpcomingEvents.json")) {
                noteDataVector = FixUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<EventNoteData>>() {
                });
            } else if ((theFilename.startsWith("M") && theFilename.charAt(3) == '_')) {
                noteDataVector = FixUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<NoteData>>() {
                });
            } else if ((theFilename.startsWith("Y") && theFilename.charAt(1) == '_')) {
                noteDataVector = FixUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<NoteData>>() {
                });
            } else if ((theFilename.startsWith("D") && theFilename.charAt(5) == '_')) {
                noteDataVector = FixUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<DayNoteData>>() {
                });
            } else if (theFilename.startsWith("todo_")) {
                noteDataVector = FixUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<OldTodoNoteData>>() {
                });
            } else if (theFilename.startsWith("search_")) {
                noteDataVector = FixUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<SearchResultData>>() {
                });
            }
        }

        // See if we can get a date for this file/path -
        String theFileDateString = "No Date from Filename";
        Date dateTmp = FixUtil.getDateFromFilename(dataFile);
        // The .toString() method seems to give a different answer, depending on TimeZone.
        // This happens even when the date is not reacquired.
        // So - we will turn it into a String right here.
        if (dateTmp != null) {
            theFileDateString = dateTmp.toString();
        }


        // Now get on with the other fixes -
        int index = -1;
        for (NoteData ndTemp : noteDataVector) {
            index++;

            // This part is just a change of name of a still-Date member; no conversion to ZDT.
            //
            // This fix needed to handle the removal of all get/set NoteDate methods,
            // and that was fallout from removing the dateNoteWhen from SearchResults,
            // which was done because 1) it didn't make sense to use this field to hold
            // both calendar-historical dates along with possible future events (Events
            // and Todo items), and 2) its usage was to be a sort key for the Found In
            // column of the Search Results, which we no longer try to sort by.
            // But a TodoNoteData still needs to hold the TMC date, if one is set.  The
            // TodoNoteData class was changed and a new name was given to the member,
            // so we need both the old and new versions of the Todo NoteData.
            if (ndTemp instanceof OldTodoNoteData) {
                OldTodoNoteData otnd = (OldTodoNoteData) ndTemp;
                TodoNoteData tnd = new TodoNoteData(otnd.copy());

                // The above gave us the NoteDate basic members; still need todo-specific
                tnd.setTodoDate(otnd.getNoteDate());
                tnd.setPriority(otnd.getPriority());
                tnd.setStatus(otnd.getStatus());
                noteDataVector.setElementAt(tnd, index);
            }

            // If we have a Date for the Note, then set its zonedDateTimeLastMod to this date.
            // But we need timezones as well - as I am the only 'real' user at this stage of
            // the app, we will set timezones according to where I was at those times (in
            // broad strokes, of course).
            if (dateTmp != null) {
                System.out.println("  Date from the filename: " + theFileDateString);

                // Before we set a TimeZone, get the right Date.

                // Where I lived for a given time period, for time-zoning.
                // Jul 1978 - Sierra Vista  (one note, here)
                // Aug 1980 - Apr 1992 Various California
                // Apr 1992 - Oct 2004 Sierra Vista and Tucson
                // Oct 2004 - May 2008 San Diego / Chula Vista
                // May 2008 - Jul 2019 Austin, TX
                // Jul 2019 - present  Izhevsk (SAMT)
                //
                TimeZone theZone;
                if (dateTmp.before(earlyAZ)) theZone = tzone0;      // 78-79
                else if (dateTmp.before(firstCA)) theZone = tzone1; // 80-92
                else if (dateTmp.before(back2AZ)) theZone = tzone0; // 92-2004
                else if (dateTmp.before(back2CA)) theZone = tzone1; // 2004-2008
                else if (dateTmp.before(austin)) theZone = tzone2;  // 2008-2019
                else theZone = tzone3;  // 7/2019 to present

                // Now, if this date-based note is a DAY note then we can set the time portion as well.
                // But recall - the timeOfDayDate is a java.util.Date that really only holds the time; the
                // date portion of it cannot be trusted, at all.  Dates for all calendar-based notes to
                // this point have only been faithfully retained in their FILE NAME!
                if (ndTemp instanceof DayNoteData) {
                    // cast it to a DayNoteData
                    DayNoteData dnd = (DayNoteData) ndTemp;

                    // and get the Date where its time was set -
                    Date theTimeOnly = dnd.getTimeOfDayDate();
                    if (theTimeOnly == null) {
                        // Ok so we don't have a time to set, but we still want to fix the TZ of the LMD
                        System.out.println("The time on this Note was cleared!");

                        // We have to set the setNoteDateTime to null, otherwise it still has the one from the DayNoteData constructor.
                        dnd.setNoteDateTime(null);

                        // Repeating the section for Month and Year notes (in the 'else' for 'if DayNoteData'), but it works.
                        ZonedDateTime zdt1 = ZonedDateTime.ofInstant(dateTmp.toInstant(), theDefaultZone.toZoneId());
                        System.out.println("  zdt1: " + zdt1);
                        ZonedDateTime zdt2 = getLongZonedDateTime(dateTmp, theZone);
                        // zdt1 gave us the right Date, zdt2 gave the right TimeZone.
                        StringBuilder theLMD = new StringBuilder();
                        theLMD.append(zdt1.toString().substring(0, 11)).append("00:00");
                        theLMD.append(zdt2.toString().substring(23));

                        // Now, set the zdtLastMod - a bad thing, but this fixer will be the only one to ever use
                        // the method (for my/test data only), and  then it will be removed.
                        System.out.println("  Last Mod ZonedDateTime for this note: " + theLMD);
                        ZonedDateTime zdt = ZonedDateTime.parse(theLMD); // Testing the result, not used.
                        System.out.println();
                        ndTemp.setBadBadThing(theLMD.toString());
                    } else {
                        // We have a time for this Note - preserve it into the new String.
                        System.out.println("  The original Note Date (we only want the time), to string: " + theTimeOnly.toString());

                        // Now extract just the time portion of that:  HH:mm
                        TimeZone.setDefault(theZone);
                        String theTime = FixUtil.makeTimeString(theTimeOnly);
                        System.out.println("  Time from FixUtil makeTimeString: " + theTime);
                        TimeZone.setDefault(theDefaultZone);

                        // This one will give us the correct date -
                        ZonedDateTime zdt1 = ZonedDateTime.ofInstant(dateTmp.toInstant(), theDefaultZone.toZoneId());
                        System.out.println("  zdt1: " + zdt1);

                        // This one will give us the TimeZone that I was living in on the date of the Note.
                        ZonedDateTime zdt2 = ZonedDateTime.ofInstant(dateTmp.toInstant(), theZone.toZoneId());
                        int millis = zdt2.get(ChronoField.MILLI_OF_SECOND);
                        if (millis == 0) {
                            // If there are no milliseconds, our substring parsing below will fail miserably.
                            // ... and don't even get me started on microseconds and nanoseconds; there better not be any.
                            System.out.println("  No milliseconds!  Adding one.");
                            zdt2 = zdt2.plus(1, ChronoField.MILLI_OF_DAY.getBaseUnit());
                        } else {
                            System.out.println("  Milliseconds detected: " + millis);
                        }
                        System.out.println("  zdt2: " + zdt2);


                        // Now build the final zdt string with the correct date, time, and zone:
                        StringBuilder zdtString = new StringBuilder();
                        String s1 = zdt1.toString().substring(0, 11);
                        System.out.println("  The Date part: " + s1);
                        System.out.println("  The Time part: " + theTime);
                        String s3 = zdt2.toString().substring(23);
                        System.out.println("  The TimeZone part: " + s3);

                        zdtString.append(s1);
                        zdtString.append(theTime);
                        zdtString.append(s3);
                        //System.out.println("  The final ZonedDateTime String: " + zdtString);
                        ZonedDateTime zdtTheFinal = ZonedDateTime.parse(zdtString);

                        // Set the value of the 'new' field - this time, both the date and the time are correct, along with the TimeZone.
                        System.out.println("  Called setNoteDateTime to: " + zdtString);
                        dnd.setNoteDateTime(zdtTheFinal);

                        // Now, set the zdtLastMod - a bad thing, but this fixer will be the only one to ever use
                        // the method (for my/test data only), and  then it will be removed.
                        System.out.println("  Last Mod ZonedDateTime for this note: " + zdtTheFinal.toString());
                        System.out.println();
                        ndTemp.setBadBadThing(zdtTheFinal.toString());
                    } // end else - timeOfDayDate not null
                } else {  // MonthNotes and YearNotes
                    // Get the ZonedDateTime for the TimeZone where I was, at the time this note describes
                    // (regardless, I know, of when it was actually written).
                    // This has the effect of possibly changing the day (therefore the month and year as well)
                    // but unlike the heroic effort we undertook for DayNotes, here we are only affecting the
                    // last mod date, which we ALREADY accept as flawed.  So what we are really getting here
                    // is not a particularly good LMD, but at least we're getting one that is more likely to
                    // be in the right TimeZone.
                    ZonedDateTime zdt1 = ZonedDateTime.ofInstant(dateTmp.toInstant(), theDefaultZone.toZoneId());
                    System.out.println("  zdt1: " + zdt1);
                    ZonedDateTime zdt2 = getLongZonedDateTime(dateTmp, theZone);
                    // zdt1 gave us the right Date, zdt2 gave the right TimeZone.
                    StringBuilder theLMD = new StringBuilder();
                    theLMD.append(zdt1.toString().substring(0, 11)).append("00:00");
                    theLMD.append(zdt2.toString().substring(23));

                    // Now, set the zdtLastMod - a bad thing, but this fixer will be the only one to ever use
                    // the method (for my/test data only), and  then it will be removed.
                    System.out.println("  Last Mod ZonedDateTime for this note: " + theLMD);
                    ZonedDateTime zdt = ZonedDateTime.parse(theLMD); // Testing the result, not used.
                    System.out.println();
                    ndTemp.setBadBadThing(theLMD.toString());
                } // end if this note is a DayNoteData
            } // end if we have a Date

        } // end for each Note in the Vector

        // Put the adjusted data Vector back into the object array that was loaded from file.
        assert theGroupData != null;
        theGroupData[theGroupData.length - 1] = noteDataVector;

        // And then save the object array back into the file it came from.
        int nw = FixUtil.saveNoteGroupData(theAbsolutePath, theGroupData);
        System.out.println("   wrote " + nw + " notes to " + theFilename);


    }//end searchDataFile

    ZonedDateTime getLongZonedDateTime(Date theDate, TimeZone theZone) {
        ZonedDateTime zdt2 = ZonedDateTime.ofInstant(theDate.toInstant(), theZone.toZoneId());
        int millis = zdt2.get(ChronoField.MILLI_OF_SECOND);
        if (millis == 0) {
            // If there are no milliseconds, our substring parsing below will fail miserably.
            // ... and don't even get me started on microseconds and nanoseconds; there better not be any.
            System.out.println("  No milliseconds!  Adding one.");
            zdt2 = zdt2.plus(1, ChronoField.MILLI_OF_DAY.getBaseUnit());
        } else {
            System.out.println("  Milliseconds detected: " + millis);
        }
        System.out.println("  zdt2: " + zdt2);
        return zdt2;
    }


    public static void main(String[] args) {
        MemoryBank.debug = true; // Turn on all debugging printouts.
        //MemoryBank.setUserDataHome("test.user@lcware.net");
        //MemoryBank.setUserDataHome("g02@doughmain.net");
        //MemoryBank.setUserDataHome("jondo.nonamus@lcware.net");
        MemoryBank.setUserDataHome("lex@doughmain.net");

        DataFix dataFix = new DataFix();
        dataFix.scanDataDir(new File(MemoryBank.userDataHome), 0);
    }
}
