//
//  Needed so that the TextPage may call these methods without
//  regard to the type of 'parent'.
//

public interface TextPageListener {
    void fontChanged();

    void pageChanged();
} // end TextPageListener

