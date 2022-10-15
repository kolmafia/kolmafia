const { print, sessionLogs } = require("kolmafia");

sessionLogs(-1); // this will throw an exception
print("This code should not be reached.");