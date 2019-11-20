import javax.swing.*;

public class Area51 {
    private Notifier optionPane = new Notifier() {};

    static {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("lex@doughmain.net");
    }

    private Area51() {
    }

    // Developing a new picklist widget, to replace a system-native File chooser.  It
    // looks like all that is needed is a direct usage of JOptionPane.  argh!
    private void try1() {
        AppIcon theIcon = new AppIcon("icons/acro.ico");

        String[] nums = {
                "00", "01", "02", "03", "04", "05", "06", "07", "08", "09",
                "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
                "20", "21", "22", "23", "24", "25", "26", "27", "28", "29",
                "30", "31", "32", "33", "34", "35", "36", "37", "38", "39",
                "40", "41", "42", "43", "44", "45", "46", "47", "48", "49",
                "50", "51", "52", "53", "54", "55", "56", "57", "58", "59",
                "60", "61", "62", "63", "64", "65", "66", "67", "68", "69",
                "70", "71", "72", "73", "74", "75", "76", "77", "78", "79",
                "80", "81", "82", "83", "84", "85", "86", "87", "88", "89",
                "90", "91", "92", "93", "94", "95", "96", "97", "98", "99"};


        String theChoice = optionPane.showInputDialog(null, "Choose a group",
                "Group Chooser", JOptionPane.PLAIN_MESSAGE, null, nums, "89");

        System.out.println("The choice is: " + theChoice);
    }


    public static void main(String[] args) {
        Area51 a51 = new Area51();
        a51.try1();

    }

}

