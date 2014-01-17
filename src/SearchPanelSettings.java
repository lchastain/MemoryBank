import java.io.Serializable;
import java.util.Date;

// This class holds the Search parameters.
//-----------------------------------------------------------------------
class SearchPanelSettings implements Serializable {
    public static final long serialVersionUID = -7535946261231395347L;

    public boolean not1;
    public boolean not2;
    public boolean not3;
    public String word1;
    public String word2;
    public String word3;
    public boolean and1;
    public boolean and2;
    public boolean or1;
    public boolean or2;
    public boolean paren1;
    public boolean paren2;

    public Date dateWhen1;
    public Date dateWhen2;
    public int whenChoice;

    public Date dateMod1;
    public Date dateMod2;
    public int modChoice;

} // end SearchPanelSettings
