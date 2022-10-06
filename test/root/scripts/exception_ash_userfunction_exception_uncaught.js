const { print } = require("kolmafia");
const helper = require("exception_ash_userfunction_helper.ash");

print(helper.returnsEleven());
helper.throwScriptException();
print("This code should not be reached.");