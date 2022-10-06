const { printHtml } = require("kolmafia");

[
  'Ed the Undying',
  'warwelf',
  'lihc',
  'ancient protector spirit'
].forEach((name) => {
  const monster = Monster.get(name);

  printHtml(JSON.stringify({
    name,
    attackElement: (monster.attackElement || '').toString(),
    attackElements: monster.attackElements.map((element) => element.toString()),
    images: monster.images,
    randomModifiers: monster.randomModifiers,
    subTypes: monster.subTypes,
  }, null, 2));
});

[
  'Beaten up',
  'Tomato Power'
].forEach((name) => {
  const effect = Effect.get(name);

  printHtml(JSON.stringify({
    name,
    all: effect.all,
  }, null, 2));
});