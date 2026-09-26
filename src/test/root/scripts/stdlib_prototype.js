const { print, isTradeable } = require('kolmafia');

const item = Item.get("seal tooth");

const tests = {
    constructor: () => isTradeable.constructor === Function.prototype.constructor,
    call: () => isTradeable.call(null, item) === true,
    apply: () => isTradeable.apply(null, [item]) === true,
    bind: () => {
        const bound = isTradeable.bind(null);
        return bound(item) === true;
    },
    toString: () => isTradeable.toString().length > 0,
    toSource: () => isTradeable.toSource().length > 0,
}

Object.entries(tests).forEach(([k, v]) => print(`${k}: ${v()}`));