This directory - 'null' is here so that when userDataHome is null, 
tests will encounter an expected Exception when trying to access
files or directories.  

Content types are likely to be reversed, in order to cause the Exceptions -
ie, an expected file will actually be a directory, etc.