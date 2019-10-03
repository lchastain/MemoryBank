import java.time.LocalTime;
import java.time.ZonedDateTime;

public class Area51 {

    private Area51() {
    }

    private void try1() {
        LocalTime lt = LocalTime.now();
        System.out.println("New java.time.LocalTime is: " + lt);
    }

    // Testing conversion of a ZonedDateTime to LocalDateTime
    private void try2() {
        // This test needs 'our' timezone to not be the same as the one in
        // our zdtLastModString, below.  But we just verify that visually.

        // Show our current timezone
        System.out.println("ZonedDateTime.now(): " + ZonedDateTime.now());

        // Parse our string into a ZonedDateTime, print out to verify.
        String zdtLastModString = "2009-02-14T19:42-06:00[America/Chicago]";
        ZonedDateTime zdt = ZonedDateTime.parse(zdtLastModString);

        // Convert to LocalDateTime, verify the time did not change.
        System.out.println("ZonedDateTime: " + zdt);
        System.out.println("  to LocalDateTime: " + zdt.toLocalDateTime());

        // Output:
//        ZonedDateTime.now(): 2019-10-03T09:40:38.308+04:00[Europe/Samara]
//        ZonedDateTime: 2009-02-14T19:42-06:00[America/Chicago]
//          to LocalDateTime: 2009-02-14T19:42

        // This shows that this conversion DOES NOT adjust the original time
        // to be aligned with the current timezone.  Good.
    }




    public static void main(String[] args) {
        Area51 a51 = new Area51();

        a51.try2();

    }


}
