const { print } = require("kolmafia");
const helper = require("exception_ash_userfunction_helper.ash");

try {
  print(helper.returnsEleven());
  helper.throwScriptException();
  print("This code should not be reached.");
} catch (e) {
  print("Caught exception: " + e.message);
}
