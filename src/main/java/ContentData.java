import java.util.Vector;

class ContentData {
    String contentTextFragment;
    Vector<AttributeData> contentAttributeData;

    ContentData() {
        contentTextFragment = "";
        contentAttributeData = new Vector<>(0, 1);
    }

    ContentData(String theText) {
        this();
        contentTextFragment = theText;
    }

}
