const {print, toString: kolmafiaToString} = require("kolmafia");

[
  11,
  11.0,
  2147483647, // Java Integer.MAX_VALUE
  2147483648, // Java Integer.MAX_VALUE + 1
  2147483648.0,
  11_111_111_111,
  9007199254740991, // JavaScript Number.MAX_SAFE_INTEGER
].forEach((value) => {
  const asString = String(value);
  const asKolmafiaString = kolmafiaToString(value);
  const delimitedString = kolmafiaToString(value, "%,d");
  print(JSON.stringify({
    value,
    isSafeInteger: Number.isSafeInteger(value),
    asString,
    asKolmafiaString,
    stringsEqual: asString === asKolmafiaString,
    delimitedString,
  }));
});

print("");

[
  9007199254740992, // JavaScript Number.MAX_SAFE_INTEGER + 1, this will not be converted to a long, but to a double
  11.5, // this will be a double
  1.1, // this will be a double
  2147483648.5, // this will be a double
].forEach((value) => {
  const asString = String(value);
  const asKolmafiaString = kolmafiaToString(value);
  print(JSON.stringify({
    value,
    isSafeInteger: Number.isSafeInteger(value),
    asString,
    asKolmafiaString,
    stringsEqual: asString === asKolmafiaString,
  }));
});