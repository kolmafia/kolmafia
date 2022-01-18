const { print, toJson, itemDropsArray } = require('kolmafia');

let monster = Monster.get("beanbat");
let drops = itemDropsArray(monster);

print(`${monster} has ${drops.length} drops.`);
print(`The first drop is ${drops[0].drop} with a rate of ${drops[0].rate}.`);
print(`Its image is ${drops[0].drop.image}.`);
