const { sellCost, print } = require("kolmafia");

// Wal-Mart	buy	15	ice wine	ROW765
// 765	glaciest	ice wine	Wal-Mart gift certificate (15)
const iceWine = sellCost(Coinmaster.get("Wal-Mart"), Item.get("ice wine"));
print(`Buy, item, single currency: ${Object.keys(iceWine).length === 1 && Object.entries(iceWine).every(([k, v]) => k === "Wal-Mart gift certificate" && v === 15)}`);

// Genetic Fiddling	ROW867	Flappy Ears	rad (60)
// 867	mutate	Flappy Ears	rad (60)
const flappyEars = sellCost(Coinmaster.get("Genetic Fiddling"), Skill.get("Flappy Ears"));
print(`Shoprow, skill, single currency: ${Object.keys(flappyEars).length === 1 && Object.entries(flappyEars).every(([k, v]) => k === "rad" && v === 60)}`);

// Crimbo24 Cafe	ROW1532	holiday smorgasbord	Spirit of Easter (15)	Spirit of St. Patrick's Day (15)	Spirit of Veteran's Day (15)	Spirit of Thanksgiving (15)	Spirit of Christmas (15)
// 1532	crimbo24_cafe	holiday smorgasbord	Spirit of Easter (15)	Spirit of St. Patrick's Day (15)	Spirit of Veteran's Day (15)	Spirit of Thanksgiving (15)	Spirit of Christmas (15)
const smorg = sellCost(Coinmaster.get("Crimbo24 Cafe"), Item.get("holiday smorgasbord"));
print(`Shoprow, item, multiple currencies: ${Object.keys(smorg).length === 5 && Object.entries(smorg).every(([k, v]) => k.startsWith("Spirit of") && v === 15)}`);
