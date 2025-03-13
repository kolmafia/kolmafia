const { phpSeed, phpMtRand, phpRand, print } = require('kolmafia');

const r = phpSeed(1);
const s = phpSeed(2147483647);
print(phpMtRand(r));
print(phpMtRand(s));
print(phpMtRand(r));
print(phpMtRand(s));

r = phpSeed(1);
s = phpSeed(2147483647);
print(phpRand(r));
print(phpRand(s));
print(phpRand(r));
print(phpRand(s));
