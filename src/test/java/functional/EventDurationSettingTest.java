import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

// This class is for testing the setDurationValue method of an EventNoteData.
// There are 16 different major data variants (with 4 each minor variants) as
// to how that method can be executed, and since these tests need to fully
// exercise them all, they are considered functional testing rather than Unit,
// even though we are testing a single class method.

// Covers testing needed to confirm parts of the resolution for SCR0089.

class EventDurationSettingTest {
    private EventNoteData eventNoteData;


    @BeforeEach
    void setUp() {
        eventNoteData = new EventNoteData();
        Assertions.assertNull(eventNoteData.getStartDate());
        Assertions.assertNull(eventNoteData.getStartTime());
        Assertions.assertNull(eventNoteData.getEndDate());
        Assertions.assertNull(eventNoteData.getEndTime());
        EventNoteData.settingDuration = true;  // That's what we're doing here; over and over again.
    }

    @AfterAll
    static void tearDown() {
        // This might affect other tests being run at the same time, so setting it back now.
        EventNoteData.settingDuration = false;
    }

    // ALL Unknown
    // Test that when a Duration is entered while no Start/End Date/Time is known,
    // none of the unknowns is (re-)calculated, and the Duration value is retained.
    @Test
    void testAllUnknown() {
        eventNoteData.setDurationValue(45);
        eventNoteData.setDurationUnits("Hours");

        Assertions.assertNull(eventNoteData.getStartDate());
        Assertions.assertNull(eventNoteData.getStartTime());
        Assertions.assertNull(eventNoteData.getEndDate());
        Assertions.assertNull(eventNoteData.getEndTime());

        // Verify that the setting was accepted.  Subsequent tests in this
        // class will not need to keep repeating this check.
        Assertions.assertEquals(Integer.valueOf(45), eventNoteData.getDurationValue());
    }

    // END_TIME is known.
    // When only END_TIME is known, verify that START_TIME is set when a Duration is entered.
    @Test
    void testEndTimeKnown() {
        // Set the known End Time.
        eventNoteData.setEndTime(LocalTime.of(8, 47));

        LocalTime theStartTime;
        eventNoteData.setDurationValue(45); // We can use this value throughout.

        // Expect Start Time to go back by 45 minutes from End Time.
        eventNoteData.setDurationUnits("Minutes");
        theStartTime = eventNoteData.getStartTime();
        Assertions.assertEquals(LocalTime.of(8, 2), theStartTime);

        // Expect Start Time to go back by 45 hours from End Time.
        eventNoteData.setStartTime(null); // reset the test; put Start Time back to unknown.
        eventNoteData.setDurationUnits("Hours");
        theStartTime = eventNoteData.getStartTime();
        Assertions.assertEquals(LocalTime.of(11, 47), theStartTime);

        // Expect Start Time to equal End Time because Duration is in Days
        eventNoteData.setStartTime(null); // reset the test; put Start Time back to unknown.
        eventNoteData.setDurationUnits("daYS"); // This also tests the input normalization for casing.
        theStartTime = eventNoteData.getStartTime();
        Assertions.assertEquals(LocalTime.of(8, 47), theStartTime);

        // Expect Start Time to equal End Time because Duration is in Weeks
        eventNoteData.setStartTime(null); // reset the test; put Start Time back to unknown.
        eventNoteData.setDurationUnits("WEEkS"); // This also tests the input normalization for casing.
        theStartTime = eventNoteData.getStartTime();
        Assertions.assertEquals(LocalTime.of(8, 47), theStartTime);

        // Verify Start Date was not affected.
        Assertions.assertNull(eventNoteData.getStartDate());
    }

    // END_DATE is known.
    // When only END_DATE is known, verify that START_DATE is set when a Duration of Days or
    // Weeks is entered.  If Hours or Minutes are entered and they can be converted to days
    // with no remainder then that number of days will be used, otherwise no Start Date will be set.
    @Test
    void testEndDateKnown() {
        // Set the known End Date.
        LocalDate theEndDate = LocalDate.of(2018, 8, 10);
        eventNoteData.setEndDate(theEndDate);

        // Set a duration
        eventNoteData.setDurationValue(15);

        // Test Days
        eventNoteData.setDurationUnits("Days");
        Assertions.assertEquals(LocalDate.of(2018, 7, 26), eventNoteData.getStartDate());
        eventNoteData.setStartDate(null); // reset the test; put Start Date back to unknown.

        // Test Weeks
        eventNoteData.setDurationUnits("WeeKS");
        Assertions.assertEquals(LocalDate.of(2018, 4, 27), eventNoteData.getStartDate());
        eventNoteData.setStartDate(null); // reset the test; put Start Date back to unknown.

        // Test Hours
        eventNoteData.setDurationUnits("houRs");
        Assertions.assertNull(eventNoteData.getStartDate());  // 15 hrs is not a whole day; no setting of Start Date should occur.
        eventNoteData.setDurationValue(75); // 3 days and 3 hours - more than a day but shouldn't be counted.
        Assertions.assertNull(eventNoteData.getStartDate());  // without knowing the End Time, it could be 3 days earlier, or 4.  So - verify no effect.
        eventNoteData.setDurationValue(72); // Now this one is divisible by 24 hours with no remainder, to equal 3 days -
        Assertions.assertEquals(LocalDate.of(2018, 8, 7), eventNoteData.getStartDate());
        eventNoteData.setStartDate(null); // reset the test; put Start Date back to unknown.

        // Test Minutes
        eventNoteData.setDurationUnits("minutes");  // 72 minutes is not a whole day; no setting of Start Date should occur.
        Assertions.assertNull(eventNoteData.getStartDate());
        eventNoteData.setDurationValue(4335); // 3 days and 15 minutes - more than a day but shouldn't be counted.
        Assertions.assertNull(eventNoteData.getStartDate());  // without knowing the End Time, it could be 3 days earlier, or 4.  So - verify no effect.
        eventNoteData.setDurationValue(7200); // Now this one is divisible by minutes in a day with no remainder, to equal 5 days -
        Assertions.assertEquals(LocalDate.of(2018, 8, 5), eventNoteData.getStartDate());
    }

    // END_DATE & END_TIME are known.
    // END_DATE & END_TIME & START_TIME are known.
    // END_DATE & END_TIME & START_DATE are known.
    // Test that when a Duration is entered and both the End Date and End Time are known,
    // the Start Date and Start Time are set properly.  This also tests when Start Date
    // is known but will be overridden, or the Start Time is known but will be overwritten.
    @Test
    void testEndDateAndEndTimeKnown() {
        // Set the known End Date and Time.
        LocalDate theEndDate = LocalDate.of(2018, 10, 8);
        eventNoteData.setEndDate(theEndDate);
        LocalTime theEndTime = LocalTime.of(16,20);
        eventNoteData.setEndTime(theEndTime);

        // Set a duration value.  We can use this throughout the rest of this test.
        eventNoteData.setDurationValue(17);

        // Test Duration in Minutes
        eventNoteData.setDurationUnits("minutes");
        Assertions.assertEquals(theEndDate, eventNoteData.getStartDate()); // Same day.
        Assertions.assertEquals(theEndTime.minusMinutes(17), eventNoteData.getStartTime() );
        eventNoteData.setStartDate(null); // reset the test; put Start Date back to unknown.
        eventNoteData.setStartTime(null); // reset the test; put Start Time back to unknown.

        // Test Duration in Hours
        eventNoteData.setDurationUnits("hours");
        Assertions.assertEquals(LocalDate.of(2018, 10, 7), eventNoteData.getStartDate()); // One day earlier
        Assertions.assertEquals(LocalTime.of(23,20), eventNoteData.getStartTime());
        eventNoteData.setStartDate(null); // reset the test; put Start Date back to unknown.
        eventNoteData.setStartTime(null); // reset the test; put Start Time back to unknown.

        // Test Duration in Days
        eventNoteData.setDurationUnits("DAyS");
        Assertions.assertEquals(LocalDate.of(2018, 9, 21), eventNoteData.getStartDate());
        Assertions.assertEquals(theEndTime, eventNoteData.getStartTime()); // No effect on Time
        eventNoteData.setStartDate(null); // reset the test; put Start Date back to unknown.
        eventNoteData.setStartTime(null); // reset the test; put Start Time back to unknown.

        // Test Duration in Weeks
        eventNoteData.setDurationUnits("WeekS");
        Assertions.assertEquals(LocalDate.of(2018, 6, 11), eventNoteData.getStartDate());
        Assertions.assertEquals(theEndTime, eventNoteData.getStartTime()); // No effect on Time
    }

    // START_TIME is known.
    // START_TIME & END_TIME are known.
    // Test that when a Duration is entered while the Start Time is known,
    // the unknown End Time is set properly or the previously determined
    // End Time is overridden.  See the notes for this case in the data class.
    @Test
    void testStartTimeKnown() {
        // Set the known Start Time.
        eventNoteData.setStartTime(LocalTime.of(8, 47));

        LocalTime theEndTime;
        eventNoteData.setDurationValue(45); // We can use this value throughout.

        // Expect End Time to go ahead by 45 minutes from Start Time.
        eventNoteData.setDurationUnits("Minutes");
        theEndTime = eventNoteData.getEndTime();
        Assertions.assertEquals(LocalTime.of(9, 32), theEndTime);
        eventNoteData.setEndTime(null); // reset the test; put End Time back to unknown.

        // Expect Start Time to go ahead by 45 hours from End Time.
        eventNoteData.setDurationUnits("Hours");
        theEndTime = eventNoteData.getEndTime();
        Assertions.assertEquals(LocalTime.of(5, 47), theEndTime);
        eventNoteData.setEndTime(null); // reset the test; put End Time back to unknown.

        // Expect Start Time to equal End Time because Duration is in Days
        eventNoteData.setDurationUnits("daYS"); // This also tests the input normalization for casing.
        theEndTime = eventNoteData.getEndTime();
        Assertions.assertEquals(LocalTime.of(8, 47), theEndTime);
        eventNoteData.setEndTime(null); // reset the test; put End Time back to unknown.

        // Expect Start Time to equal End Time because Duration is in Weeks
        eventNoteData.setDurationUnits("WEEkS"); // This also tests the input normalization for casing.
        theEndTime = eventNoteData.getEndTime();
        Assertions.assertEquals(LocalTime.of(8, 47), theEndTime);

        // Verify End Date was not affected.
        Assertions.assertNull(eventNoteData.getEndDate());
    }

    // START_TIME & END_DATE are known.
    // Test that when START_TIME & END_DATE are known, then END_TIME & START_DATE are set.
    @Test
    void testStartTimeAndEndDateKnown() {
        // Set the knowns.
        LocalTime startTime = LocalTime.of(12, 37);
        eventNoteData.setStartTime(startTime);
        LocalDate endDate = LocalDate.of(2017, 3, 23);
        eventNoteData.setEndDate(endDate);

        eventNoteData.setDurationValue(3); // We can use this for all Units.

        // Test Weeks
        eventNoteData.setDurationUnits("weeks");
        Assertions.assertEquals(LocalDate.of(2017,3,2), eventNoteData.getStartDate());
        Assertions.assertEquals(LocalTime.of(12,37), eventNoteData.getEndTime());
        eventNoteData.setEndTime(null);   // reset the test; put End Time back to unknown.
        eventNoteData.setStartDate(null); // reset the test; put Start Date back to unknown.

        // Test Days
        eventNoteData.setDurationUnits("days");
        Assertions.assertEquals(LocalDate.of(2017,3,20), eventNoteData.getStartDate());
        Assertions.assertEquals(LocalTime.of(12,37), eventNoteData.getEndTime());
        eventNoteData.setEndTime(null);   // reset the test; put End Time back to unknown.
        eventNoteData.setStartDate(null); // reset the test; put Start Date back to unknown.

        // Test Hours
        eventNoteData.setDurationUnits("hours");
        Assertions.assertEquals(LocalDate.of(2017,3,23), eventNoteData.getStartDate());
        Assertions.assertEquals(LocalTime.of(15,37), eventNoteData.getEndTime());
        eventNoteData.setEndTime(null);   // reset the test; put End Time back to unknown.
        eventNoteData.setStartDate(null); // reset the test; put Start Date back to unknown.

        // Test Minutes
        eventNoteData.setDurationUnits("minutes");
        Assertions.assertEquals(LocalDate.of(2017,3,23), eventNoteData.getStartDate());
        Assertions.assertEquals(LocalTime.of(12,40), eventNoteData.getEndTime());
    }

    // START_DATE is known.
    // START_DATE & END_DATE are known.
    // Test that when a Start Date is known and the user enters a Duration, End Date will be set if the duration
    // units are Days or Weeks.  If the duration is in Minutes or Hours and they can be converted to whole days
    // with no remainder then End Date will be set using that number of duration days.  Otherwise no End Date is set
    // unless it is already known, in which case it may need to be nulled out if it would otherwise be illogical
    // given the known start and new duration.
    @Test
    void testStartDateKnown() {
        // Set the known Start Date.
        LocalDate theStartDate = LocalDate.of(2018, 8, 10);
        eventNoteData.setStartDate(theStartDate);

        // Set a duration
        eventNoteData.setDurationValue(15);

        // Test Days
        eventNoteData.setDurationUnits("Days");
        Assertions.assertEquals(LocalDate.of(2018, 8, 25), eventNoteData.getEndDate());
        eventNoteData.setEndDate(null); // reset the test; put End Date back to unknown.

        // Test Weeks
        eventNoteData.setDurationUnits("WeeKS");
        Assertions.assertEquals(LocalDate.of(2018, 11, 23), eventNoteData.getEndDate());
        eventNoteData.setEndDate(null); // reset the test; put End Date back to unknown.

        // Test Hours
        eventNoteData.setDurationUnits("hours");  // 15 hrs is not a whole day; no setting of End Date should occur.
        Assertions.assertNull(eventNoteData.getEndDate());
        eventNoteData.setDurationValue(75); // This one is divisible by 24 hours (but with a remainder) to equal 3 days and 3 hours
        Assertions.assertNull(eventNoteData.getEndDate()); // So it stays unknown.
        Assertions.assertTrue(eventNoteData.setEndDate(LocalDate.of(2022, 2,14))); // Set an End Date that is much greater than Start Date + Duration.
        // And set duration again so that it considers the new End Date.
        eventNoteData.setDurationValue(76); // This one is divisible by 24 hours (but with a remainder) to equal 3 days and 4 hours
        // The existing End Date is too far away from the Start plus Duration (and we have a remainder) so it was nulled out when duration was set.
        Assertions.assertNull(eventNoteData.getEndDate()); // End Date went away.
        // Now try a duration that does not cover more than one day -
        Assertions.assertTrue(eventNoteData.setEndDate(LocalDate.of(2022, 2,14))); // Set an End Date that is much greater than Start Date + Duration.
        eventNoteData.setDurationValue(19); // Less than one day, not a multiple of 24.
        Assertions.assertNull(eventNoteData.getEndDate()); // End Date went away.
        Assertions.assertTrue(eventNoteData.setEndDate(LocalDate.of(2018, 8, 11))); // Now set End Date closer to Start
        eventNoteData.setDurationValue(15); // Zero days and there is a remainder so the End Date is not overridden to a new date,
        // and the existing one is within the allowed window so it should keep its value rather than being nulled out.
        Assertions.assertEquals(LocalDate.of(2018, 8, 11), eventNoteData.getEndDate());
        eventNoteData.setDurationValue(72); // Now this one is evenly divisible by 24 hours to equal 3 days
        // So it should cause the End Date to be overridden (or set, if we had left it unknown).
        Assertions.assertEquals(LocalDate.of(2018, 8, 13), eventNoteData.getEndDate());
        eventNoteData.setEndDate(null); // reset the test; put End Date back to unknown.

        // Test Minutes
        eventNoteData.setDurationUnits("minutes");  // 72 minutes is not a whole day; no setting of End Date should occur.
        Assertions.assertNull(eventNoteData.getEndDate());
        eventNoteData.setDurationValue(7145); // This one is divisible by 1440 minutes (but with a remainder) to equal 4 days, 23 hours and 5 minutes.
        Assertions.assertNull(eventNoteData.getEndDate()); // So it stays unknown.
        Assertions.assertTrue(eventNoteData.setEndDate(LocalDate.of(2022, 2,14))); // Set an End Date that is much greater than Start Date + Duration.
        // And set duration again so that it considers the new End Date.
        eventNoteData.setDurationValue(7146); // This one is divisible by 1440 minutes (but with a remainder) to equal 4 days, 23 hours and 6 minutes.
        // The existing End Date is too far away from the Start plus Duration (and we have a remainder) so it was nulled out when duration was set.
        Assertions.assertNull(eventNoteData.getEndDate()); // End Date went away.
        // Now try a duration that does not cover more than one day -
        Assertions.assertTrue(eventNoteData.setEndDate(LocalDate.of(2022, 2,14))); // Set an End Date that is much greater than Start Date + Duration.
        eventNoteData.setDurationValue(50); // This one does not even cover one hour.
        Assertions.assertNull(eventNoteData.getEndDate()); // End Date went away.
        // Now set End Date closer to Start
        Assertions.assertTrue(eventNoteData.setEndDate(LocalDate.of(2018, 8, 15)));
        eventNoteData.setDurationValue(7145); // Zero days and there is a remainder so the End Date is not overridden to a new date,
        // and the existing one is within the allowed window so it should keep its value rather than being nulled out.
        Assertions.assertEquals(LocalDate.of(2018, 8, 15), eventNoteData.getEndDate());
        eventNoteData.setDurationValue(11520); // Now this one is evenly divisible by minutes in a day (1440), to equal 8 days.
        // So it should cause the End Date to be overridden (or set, if we had left it unknown).
        Assertions.assertEquals(LocalDate.of(2018, 8, 18), eventNoteData.getEndDate());
    }

    // START_DATE & END_TIME are known.
    // When a duration is entered, the Start Time and End Date will be set.
    @Test
    void testStartDateAndEndTimeKnown() {
        // Set the knowns.
        LocalDate startDate = LocalDate.of(2017, 3, 23);
        eventNoteData.setStartDate(startDate);
        LocalTime endTime = LocalTime.of(12, 37);
        eventNoteData.setEndTime(endTime);

        eventNoteData.setDurationValue(3); // We can use this for all Units.

        // Test Weeks
        eventNoteData.setDurationUnits("weeks");
        Assertions.assertEquals(LocalDate.of(2017,4,13), eventNoteData.getEndDate());
        Assertions.assertEquals(LocalTime.of(12,37), eventNoteData.getStartTime());
        eventNoteData.setStartTime(null);   // reset the test; put Start Time back to unknown.
        eventNoteData.setEndDate(null); // reset the test; put End Date back to unknown.

        // Test Days
        eventNoteData.setDurationUnits("days");
        Assertions.assertEquals(LocalDate.of(2017,3,26), eventNoteData.getEndDate());
        Assertions.assertEquals(LocalTime.of(12,37), eventNoteData.getStartTime());
        eventNoteData.setStartTime(null);   // reset the test; put Start Time back to unknown.
        eventNoteData.setEndDate(null); // reset the test; put End Date back to unknown.

        // Test Hours
        eventNoteData.setDurationUnits("hours");
        Assertions.assertEquals(LocalDate.of(2017,3,23), eventNoteData.getEndDate());
        Assertions.assertEquals(LocalTime.of(9,37), eventNoteData.getStartTime());
        eventNoteData.setStartTime(null);   // reset the test; put Start Time back to unknown.
        eventNoteData.setEndDate(null); // reset the test; put End Date back to unknown.

        // Test Minutes
        eventNoteData.setDurationUnits("minutes");
        Assertions.assertEquals(LocalDate.of(2017,3,23), eventNoteData.getEndDate());
        Assertions.assertEquals(LocalTime.of(12,34), eventNoteData.getStartTime());
    }


    // START_DATE & START_TIME are known.
    // START_DATE & START_TIME & END_DATE are known.
    // START_DATE & START_TIME & END_TIME are known.
    // ALL are known.
    // Test that when a Duration is entered and both the Start Date and Start Time are known,
    // the End Date and End Time are set properly.  This also tests when Start Date
    // is known but will be overridden, and/or the Start Time is known but will be overwritten.
    @Test
    void testStartDateAndStartTimeKnown() {
        // Set the known Start Date and Time.
        LocalDate theStartDate = LocalDate.of(2018, 10, 8);
        eventNoteData.setStartDate(theStartDate);
        LocalTime theStartTime = LocalTime.of(16,20);
        eventNoteData.setStartTime(theStartTime);

        // Set a duration value.  We can use this throughout the rest of this test.
        eventNoteData.setDurationValue(17);

        // Test Duration in Minutes
        eventNoteData.setDurationUnits("minutes");
        Assertions.assertEquals(theStartDate, eventNoteData.getEndDate()); // Same day.
        Assertions.assertEquals(theStartTime.plusMinutes(17), eventNoteData.getEndTime() );
        // Resets are not needed here; we will be overriding the End values whether they are null or not.

        // Test Duration in Hours
        eventNoteData.setDurationUnits("hours");
        Assertions.assertEquals(LocalDate.of(2018, 10, 9), eventNoteData.getEndDate());
        Assertions.assertEquals(LocalTime.of(9,20), eventNoteData.getEndTime());

        // Test Duration in Days
        eventNoteData.setDurationUnits("DAyS");
        Assertions.assertEquals(LocalDate.of(2018, 10, 25), eventNoteData.getEndDate());
        Assertions.assertEquals(theStartTime, eventNoteData.getEndTime()); // Same time of day, when duration is Days.
        Assertions.assertTrue(eventNoteData.setEndTime(LocalTime.of(3,43))); // A type of 'reset'

        // Test Duration in Weeks
        eventNoteData.setDurationUnits("WeekS");
        Assertions.assertEquals(LocalDate.of(2019, 2, 4), eventNoteData.getEndDate());
        Assertions.assertEquals(theStartTime, eventNoteData.getEndTime()); // Same time of day, when duration is Weeks.
    }

}
