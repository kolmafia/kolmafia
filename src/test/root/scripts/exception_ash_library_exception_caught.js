const { print, sessionLogs } = require("kolmafia");

try {
  sessionLogs(-1); // this will throw an exception
  print("This code should not be reached.");
} catch (e) {
  print("Caught exception: " + e.message);
}