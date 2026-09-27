const { Item, print } = require("kolmafia");
const helper = require("./Excluded/import_enumerated_types_helper.js");

// Available by default.
print(Effect.get("Confidence!").name);

print(Item.get("seal tooth").name);
print(typeof Item);

// Top level vars stay local to their script.
var moduleLocal = "main";
print(helper.getModuleLocal());
print(moduleLocal);
