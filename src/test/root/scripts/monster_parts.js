const { print } = require("kolmafia");

print("$monster[none] has no parts: " + (Monster.none.parts.length === 0 ? "true" : "false"));

const actual = Monster.get("Crimbuccaneer retiree").parts;
const expected = ["armpeg", "headpeg", "legpeg", "torsopeg"];

print("Reports all parts: " + (actual.length === expected.length && expected.every(p => actual.includes(p)) ? "true" : "false"));