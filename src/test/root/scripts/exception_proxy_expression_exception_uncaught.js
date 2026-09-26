const { print } = require("kolmafia");

Effect.get("this effect does not exist"); // this will throw an exception
print("This code should not be reached.");