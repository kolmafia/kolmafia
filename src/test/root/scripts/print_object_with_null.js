const {print} = require("kolmafia");

try {
  print({'key': null});
} catch (e) {
  print("exception: " + e.message);
}
