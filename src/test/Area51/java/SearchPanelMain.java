import javax.swing.*;

public class SearchPanelMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("g01@doughmain.net");

        SearchPanel theSearchPanel = new SearchPanel();

        String string1 = "Search Now";
        String string2 = "Cancel";
        Object[] options = {string1, string2};

        JOptionPane.showOptionDialog(null,
                theSearchPanel,
                "Search - Please specify the conditions for your quest",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,     //don't use a custom Icon
                options,  //the titles of buttons
                string1); //the title of the default button
    }

}
