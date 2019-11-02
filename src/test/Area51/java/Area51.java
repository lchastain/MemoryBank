import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;

public class Area51 {

    static {
        MemoryBank.setUserDataHome("test.user@lcware.net");
    }

    private Area51() {
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

    private void try3() {
        Child1 child1 = new Child1();
        Child2 child2 = new Child2();

        System.out.println("Parent string: " + ParentClass.theString);
        System.out.println("Child1 string: " + Child1.theString);
        System.out.println("Child2 string: " + Child2.theString);
    }


    public static void main(String[] args) {
        Area51 a51 = new Area51();
        a51.try3();

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
