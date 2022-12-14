# Description of the Application -  
Essentially it is just a selection tree on the left side with a menubar overall and a 
viewing area on the right side.  The tree 'grows' 
over time as you add your personal info.

The app consists of a menubar over a vertically split pane.  In the left side of the split
    pane is an application navigation tree that controls the content of the right side of
    the split pane as well as the available choices on the menubar.  The navigation tree
    has selectable branches and leaves (nodes) with descriptive names so that user
    selection is intuitive.  A listing of the available tree nodes is given below, along with
    the descriptions of the associated viewing panels and menu bar options.

## Tree Nodes:

Upcoming Events - This is a branch of the tree whose leaves correspond to Event Note
    Groups (described further below).  When this branch is selected the right side of the
    pane displays an editor where those groups on the branch may be managed.  This is one
    of three (so far) Branch Editors.  See the Branch Editors info for the full
    explanation of 'managed'.

To Do Lists

Search Results

Tree Branch Views - Currently there are two; Notes and Calendar.  No real functionality here;
    just an additional branch view of that part of the tree, but for behavioral
    consistency there is the action of showing them when the node is selected, vs otherwise
    just leaving the right side on the last selection.  These may eventually become 'editors',
    where visibility and order of the nodes can be controlled, possibly even nesting.

Upcoming Events -

Goals - nothing going on here, yet.

Year View - View a Calendar Year, one year per page, scrollable
    Which year is initially shown in the UI is described in Date Tracking
    Initial / Current date selection is shown with a red label and bold red highlight of the numeric date.

Month View - View of a (US-style) Calendar Month
    Which month is initially shown in the UI is described in Date Tracking

Day Notes   - Make / Edit / Keep up to PAGE_SIZE number of notes associated with a specific date.
    Each note has:
        A line of text shown in a (x character) text field that can scroll horizontally to hold up to x characters
        An optional time field with optional associated icons
    Shows up to PAGE_SIZE number of notes for
    Which day is shown in the UI can be controlled in several ways.  Foremost are the +/- controls,
        for incrementing and decrementing the display by one Day.
    The UI shows up to 40 notes in a day.

Month Notes - Make / Edit / Keep Month based notes

Year Notes  - Make / Edit / Keep Year based notes

Note-based interfaces:
    Day Notes (DayNoteGroupPanel)
    Month Notes (MonthNoteGroupPanel)
    Year Notes (YearNoteGroupPanel)
    Todo Lists (TodoNoteGroupPanel)
    Upcoming Events (EventNoteGroupPanel)
    Search Results (SearchResultGroupPanel)

ToDo Lists  - Make / Edit / Manage multiple named Todo Lists
    A Todo list item may be moved to Today's DayNote (via its context menu)
    A Todo list item may be assigned a related Date (via a selection on the Group's Three Month Column panel)
    A Todo list item may be moved to the DayNote of its related Date, if it has one  (via its contex menu)
    A Todo list item may be copied or moved to any other NoteGroupPanel type (via its contex menu)

==================================================

Perform a search of all user data, save resulting list and display
    search can be keyword or date-based
    results file name containing JSON data has the format:  search_<timestamp>.json
    display will show where found
    more than 40 results will be 'paged'


Branch Editors - These allow selection and deselection of the branch content to
    appear in the tree, as well as naming and order.  Associated data files may also be
    deleted.

Tree branches are
    expandable and collapsible, and their state is retained between program runs, as well
    as the last selection that was made.

[Table Of Contents](TableOfContents.md)
