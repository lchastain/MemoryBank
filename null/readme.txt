This directory - 'null' is here to support tests of 'bad' data.
When userDataHome is null, tests will encounter an expected Exception when trying to access
files or directories.  

Content types are likely to be reversed, in order to cause other Exceptions -
ie, an expected file will actually be a directory, etc.