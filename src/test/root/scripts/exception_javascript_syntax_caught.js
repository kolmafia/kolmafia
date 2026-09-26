const { print } = require("kolmafia");
try {
  require("exception_javascript_syntax_uncaught.js");
  print("This code should not be reached.");
} catch (e) {
  print("Caught exception: " + e.message);
}