const {print} = require("kolmafia");
try {
  print(3, undefined, 4);
} catch (e) {
  print("exception: " + e.message);
}
