const { print } = require("kolmafia");

try {
  Effect.get("this effect does not exist"); // this will throw an exception
  print("This code should not be reached.");
} catch (e) {
  print("Caught exception: " + e.message);
}