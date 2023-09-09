const { printHtml } = require("kolmafia");

printHtml(JSON.stringify(Item.get("lime")));
printHtml(JSON.stringify(Skill.get("Ode to Booze")));

var test = JSON.parse(JSON.stringify(Monster.get("zmobie")));
printHtml(`Flip flop: ${test.image === Monster.get("zmobie").image}`);