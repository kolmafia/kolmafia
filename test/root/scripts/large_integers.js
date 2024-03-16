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
  print(JSON.stringify({
    value,
    isSafeInteger: Number.isSafeInteger(value),
    asString,
    asKolmafiaString,
    stringsEqual: asString === asKolmafiaString,
  }));
});

print("");

[
  9007199254740992, // JavaScript Number.MAX_SAFE_INTEGER + 1, this will not be converted to a long, but to a float
  11.5, // this will be a float and can be represented
  1.1, // this will be a float, but is not a real number, so will be weird string in java
  2147483648.5, // this will be a float but is too large to be represented accurately
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