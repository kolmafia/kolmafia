const {print} = require("kolmafia");

try {
  print(null);
} catch (e) {
  print("exception: " + e.message);
}
