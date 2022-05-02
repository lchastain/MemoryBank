import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Vector;

public class Area51a {

    static {
        MemoryBank.setUserDataHome("test.user@lcware.net");
    }

    private Area51a() {
    }

    private void try1() {
        // No exception for a colon in the name, but the created file name ends with the last char before the colon.
        File f1 = new File("bad:name");
        if (f1.exists()) {
            System.out.println("wow");
        }
        System.out.println(f1.getAbsolutePath());
        try {
            boolean b = f1.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    // This shows that children cannot have different values in a static
    // string that is defined in a base class.
    private void try3() {
        System.out.println("Parent string: " + ParentClass.theString);
        System.out.println("Child1 string: " + Child1.theString);
        System.out.println("Child2 string: " + Child2.theString);
    }

    // Does isEmpty() return true if the string has only spaces?   No.
    private void try4() {
        String fourSpaces = "    ";
        System.out.println("four spaces is empty: " + fourSpaces.isEmpty());
        System.out.println("trimmed four spaces is empty: " + fourSpaces.trim().isEmpty());
    }

    // Merging two vectors, without duplicates -
    private void try5() {
        String[] stringArray1 = new String[]{"B", "D", "G"};
        String[] stringArray2 = new String[]{"A", "B", "C", "D", "E", "H"};

        Vector<String> vector1 = new Vector<>(Arrays.asList(stringArray1));
        Vector<String> vector2 = new Vector<>(Arrays.asList(stringArray2));

        LinkedHashSet<String> lhs = new LinkedHashSet<>(vector1);
        lhs.addAll(vector2);
        Vector<String> finalVector = new Vector<>(lhs);

        System.out.println(finalVector);
    }

    // What date is Feb 31?  (from Jan 31, increase the month only)
    private void try6() {
        LocalDate startDate = LocalDate.of(2022, Month.JANUARY, 31);
        LocalDate newDate = startDate.plusMonths(1);
        System.out.println(newDate);
        // Exception in thread "main" java.time.DateTimeException: Invalid date 'FEBRUARY 31'
        // LocalDate directDate = LocalDate.of(2022, Month.FEBRUARY, 31);
        System.out.println("  Plus zero years: " + newDate.plusYears(0));
    }

    public static void main(String[] args) {
        Area51a a51 = new Area51a();
        a51.try6();

    }

}

class ParentClass {
    static String theString;
}

class Child1 extends ParentClass {
    Child1() {
        theString = "area 1";
    }
}

class Child2 extends ParentClass {
    Child2() {
        theString = "area 2";
    }
}
