// TextPageListener - D. Lee Chastain 8/24/2003
//
//  Needed so that the TextPage may call these methods without
//  regard to the type of 'parent'.
//

public interface TextPageListener {
  public abstract void fontChanged();
  public abstract void pageChanged();
} // end TextPageListener

