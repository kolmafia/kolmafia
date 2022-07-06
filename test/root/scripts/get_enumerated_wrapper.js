const { print } = require("kolmafia");

let a = "li";
a += "me";
let items = Item.get([a]);

print(items[0].name);