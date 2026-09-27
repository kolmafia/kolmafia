const { print } = require("kolmafia");
const helper = require("./Excluded/shadow_globals_helper.js");

// A script may declare over anything in the global scope, ours or a builtin.
const Math = { max: () => "shadowed" };
const Effect = "not an effect";

print(Math.max(1, 2));
print(Effect);

// Other scripts keep the real ones.
print(helper.describe());

// Globals it may not declare over stay readable.
print([typeof undefined, NaN === NaN, Infinity > 0].join(" "));
