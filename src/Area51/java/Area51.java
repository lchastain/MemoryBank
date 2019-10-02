import java.time.LocalTime;

public class Area51 {

    private Area51() {
    }

    private void try1() {

        LocalTime lt = LocalTime.now();
        System.out.println("New java.time.LocalTime is: " + lt);

    }


    public static void main(String[] args) {
        Area51 a51 = new Area51();

        a51.try1();

    }


}
