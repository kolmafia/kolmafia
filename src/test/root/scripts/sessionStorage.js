const { print, sessionStorage } = require("kolmafia");

sessionStorage.setItem("control", "wow");

sessionStorage.setItem("test", "a");
sessionStorage.removeItem("test");
print(`Remove: ${sessionStorage.getItem("test") === null}`);

print(`Control: ${sessionStorage.getItem("control") === "wow"}`);

sessionStorage.setItem("test", "a");
sessionStorage.clear();
print(`Clear: ${sessionStorage.getItem("test") === null}`);