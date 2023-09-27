const { factType, print, itemFact, numericFact, effectFact } = require('kolmafia');

function item() {
    const cls = Class.get("Sauceror")
    const path = Path.get("KOLHS");
    const monster = Monster.get("Bush");

    const type = factType(cls, path, monster);

    print(`In KOLHS as a Sauceror, Bushes drops an item: ${type === "item" || type}`);

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

    print(`In Bees Hate You as a Disco Bandit, Mob Penguin Smashers drops meat: ${type === "meat" || type}`);
    const count = numericFact(cls, path, monster);
    print(`Specifically 123 meat: ${count === 123 || count}`);
}

item();
effect();
meat();