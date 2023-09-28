const { factType, print, itemFact, numericFact, effectFact, stringFact } = require('kolmafia');

function item() {
    const cls = Class.get("Sauceror")
    const path = Path.get("KOLHS");
    const monster = Monster.get("Bush");

    const type = factType(cls, path, monster);

    print(`In KOLHS as a Sauceror, Bushes drop an item: ${type === "item" || type}`);

    const item = itemFact(cls, path, monster);
    print(`That item is a line: ${item === Item.get("line") || item}`);

    const count = numericFact(cls, path, monster);
    print(`And there are 3 of them: ${count === 3 || count}`);
}

function effect() {
    const cls = Class.get("Turtle Tamer")
    const path = Path.get("Teetotaler");
    const monster = Monster.get("huge ghuol");

    const type = factType(cls, path, monster);

    print(`In Teetotal as a Turtle Tamer, huge ghuols grant an effect: ${type === "effect" || type}`);

    const effect = effectFact(cls, path, monster);
    print(`That effect is Ultrahydrated: ${effect === Effect.get("Ultrahydrated") || effect}`);

    const count = numericFact(cls, path, monster);
    print(`And it lasts 5 turns: ${count === 5 || count}`);
}

function meat() {
    const cls = Class.get("Disco Bandit")
    const path = Path.get("Bees Hate You");
    const monster = Monster.get("Mob Penguin Smasher");

    const type = factType(cls, path, monster);

    print(`In Bees Hate You as a Disco Bandit, Mob Penguin Smashers drop meat: ${type === "meat" || type}`);
    const count = numericFact(cls, path, monster);
    print(`Specifically 123 meat: ${count === 123 || count}`);
}

function allstats() {
    const cls = Class.get("Pastamancer")
    const path = Path.get("Way of the Surprising Fist");
    const monster = Monster.get("lowercase b");

    const type = factType(cls, path, monster);

    print(`In Way of the Surprising Fist as a Pastamancer, lowercase bs grant stats: ${type === "stats" || type}`);
    const count = numericFact(cls, path, monster);
    print(`Specifically 3 stats: ${count === 3 || count}`);
    const stat = stringFact(cls, path, monster);
    print(`To all stats, that is: ${stat === "all" || stat}`);
}

function stat() {
    const cls = Class.get("Sauceror")
    const path = Path.get("Slow and Steady");
    const monster = Monster.get("dairy ooze");

    const type = factType(cls, path, monster);

    print(`In Slow and Steady as a Sauceror, dairy oozes grant stats: ${type === "stats" || type}`);
    const count = numericFact(cls, path, monster);
    print(`Specifically 3 stats: ${count === 3 || count}`);
    const stat = stringFact(cls, path, monster);
    print(`To moxie, that is: ${stat === "moxie" || stat}`);
}

function itemdrop() {
    const cls = Class.get("Seal Clubber")
    const path = Path.get("Live. Ascend. Repeat.");
    const monster = Monster.get("batrat");

    const type = factType(cls, path, monster);

    print(`In Live. Ascend. Repeat. as a Seal Clubber, batrats grant an in-fight modifier: ${type === "modifier" || type}`);
    const bonus = stringFact(cls, path, monster);
    print(`Specifically 25% item drop bonus: ${bonus === "Item Drop: +25" || bonus}`);
}

[item, effect, meat, allstats, stat, itemdrop].forEach(
    (fn) => {
        fn();
        print("");
    }
);