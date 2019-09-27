import java.io.File;
import java.time.*;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.TimeZone;

public class NewDateUtil {
    private TimeZone theDefaultZone = TimeZone.getDefault(); // SAMT  - this works as long as we get it before any setting is done.
    private TimeZone tzone0 = TimeZone.getTimeZone("America/Phoenix"); // Sierra Vista, Tucson AZ
    private TimeZone tzone1 = TimeZone.getTimeZone("America/Los_Angeles"); // San Diego, Lawndale
    private TimeZone tzone2 = TimeZone.getTimeZone("America/Chicago"); // Austin, TX
    private TimeZone tzone3 = theDefaultZone; // Izhevsk, Russia

    private NewDateUtil() {
    }

    public void try1() {
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        System.out.println("ZonedDateTime now = " + zonedDateTime.toString());
        Date theDate = new Date();
        System.out.println("new Date = " + theDate.toString());

        System.out.println(AppUtil.toJsonString(theDate));

        ZonedDateTime zdt = ZonedDateTime.parse("2018-06-15T07:33:09.452+04:00[Europe/Samara]");
        System.out.println("ZonedDateTime then = " + zdt.toString());

        TimeZone.setDefault(tzone2);
        ZoneId systemDefault = ZoneId.systemDefault();
    }

    private void try2() {
        Date aNewDate = new Date();
        System.out.println("  New Java Date: " + aNewDate.toString());

        MemoryBank.tempCalendar.setTime(aNewDate);
        System.out.println("  The MB time string for it: " + MemoryBank.makeTimeString());
    }

    void try3(Date janTu) {
        System.out.println("  The original Date (we only want the time), to string: " + janTu.toString());

        MemoryBank.tempCalendar.setTime(janTu);
        System.out.println("  Time from MB makeTimeString: " + MemoryBank.makeTimeString());

        String theTime = FixUtil.makeTimeString(janTu);
        System.out.println("  Time from FixUtil makeTimeString: " + theTime);

        String theFileName = "C:\\Users\\Lee\\workspace\\Memory Bank\\appData\\test.user@lcware.net\\2018\\D0102_20190914090204.json";
        File theFile = new File(theFileName);
        System.out.println("  The file: " + theFileName);
        Date theFilenameDate = FixUtil.getDateFromFilename(theFile);
        System.out.println("  Date of the note, based on filename: " + theFilenameDate);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(theFilenameDate.toInstant(), TimeZone.getDefault().toZoneId());
        System.out.println("  ZonedDateTime (no time) of the File Date: " + zdt.toString());
        //System.out.println("  ZonedDateTime (no time) of the File Date: " + zdt.plusNanos(1000).toString());
        //System.out.println("  ZonedDateTime (no time) of the File Date: " + zdt.plusNanos(1000000).toString());
        int millis = zdt.get(ChronoField.MILLI_OF_SECOND);
        if (millis == 0) {
            // If there are no milliseconds, our substring parsing below will fail miserably.
            // ... and don't even get me started on microseconds and nanoseconds; there better not be any.
            System.out.println("No milliseconds!  Adding one.");
            zdt = zdt.plus(1, ChronoField.MILLI_OF_DAY.getBaseUnit());
        } else {
            System.out.println("Milliseconds detected: " + millis);
        }
        String zdtNoTimeString = zdt.toString();
        System.out.println("  ZonedDateTime (no time) of the File Date: " + zdtNoTimeString);

        // Now build the final zdt string with the correct date, time, and zone:
        StringBuilder zdtString = new StringBuilder();
        String s1 = zdtNoTimeString.substring(0, 11);
        System.out.println("The Date part: " + s1);
        System.out.println("The Time part: " + theTime);
        String s3 = zdtNoTimeString.substring(23);
        System.out.println("The TimeZone part: " + s3);


        zdtString.append(s1);
        zdtString.append(theTime);
        // Apparently, zeroing out the seconds and milliseconds, the ZonedDateTime drops them
        //   when printing to string, and doesn't need, when parsing.
        zdtString.append(s3);
        System.out.println("The final ZonedDateTime String: " + zdtString);
        ZonedDateTime zdtTheFinal = ZonedDateTime.parse(zdtString.toString());
        System.out.println("The final ZonedDateTime after parsing: " + zdtTheFinal);

    }

    void try4() {
        //Asia/Kuala_Lumpur +8
        ZoneId defaultZoneId = ZoneId.systemDefault();
        System.out.println("System Default TimeZone : " + defaultZoneId);

        //toString() append +8 automatically.
        Date date = new Date(1212941400000L);
        System.out.println("date : " + date);

        //1. Convert Date -> Instant
        Instant instant = date.toInstant();
        System.out.println("instant : " + instant); //Zone : UTC+0

        //2. Instant + system default time zone + toLocalDate() = LocalDate
        LocalDate localDate = instant.atZone(defaultZoneId).toLocalDate();
        System.out.println("localDate : " + localDate);

        //3. Instant + system default time zone + toLocalDateTime() = LocalDateTime
        LocalDateTime localDateTime = instant.atZone(defaultZoneId).toLocalDateTime();
        System.out.println("localDateTime : " + localDateTime);

        //4. Instant + system default time zone = ZonedDateTime
        ZonedDateTime zonedDateTime = instant.atZone(defaultZoneId);
        System.out.println("zonedDateTime : " + zonedDateTime);

    }


    public static void main(String[] args) {
//        Date janTu = new Date(1212941400000L); // Got this from 2008/D0607
//
        NewDateUtil ndu = new NewDateUtil();
//
//        System.out.println("DEFAULT Timezone:");
//        ndu.try3(janTu);
//        System.out.println();
//        System.out.println("Austin Timezone:");
//        TimeZone.setDefault(ndu.tzone2);
//        ndu.try3(janTu);
//
//        System.out.println();
//        System.out.println("Zulu Timezone:");
//        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
//        ndu.try3(janTu);

        ndu.try4();

    }


}
