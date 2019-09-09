See the DEVELOP.LOG entry for 9/8/2019, as to why this has been moved to here.
also mentioned in SCR0072.

Removed from AppTreePanel.handleMenuBar:
        else if (what.equals("Export")) doExport();

(also removed the menu item from AppMenuBar)

The export capability was an offshoot of the Data Fix capability, and looks quite similar.

The class / file Export.java is new, created to hold the code that was originally kept in
the main app, in AppTreePanel.java

Unlike the DataFix, this code was not enabled after it was moved to here;
therefore it will be at least somewhat broken.
