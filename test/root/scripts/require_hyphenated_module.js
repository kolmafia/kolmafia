// Test that modules with hyphenated filenames can be required
// This tests the URI sanitization for Java identifier validity
var k = require("kolmafia");
var mod = require("./Excluded/test-hyphenated-module.js");
module.exports.main = function() {
  k.print(mod.getMessage());
}
