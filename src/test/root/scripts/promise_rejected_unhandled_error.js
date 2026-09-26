const { print } = require("kolmafia");

function fail() {
  throw new Error("deep failure");
}

function wrapper() {
  fail();
}

new Promise((resolve, reject) => {
  try {
    resolve(wrapper());
  } catch (e) {
    reject(e);
  }
});
print("foo");
