const { print } = require("kolmafia");

// Non-configurable, so this is an error.
const undefined = 5;

print("should not get here: " + undefined);
