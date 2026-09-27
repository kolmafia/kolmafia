function fail() {
  throw new Error("deep failure");
}

function wrapper() {
  fail();
}

module.exports = {
  main: () =>
    new Promise((resolve, reject) => {
      try {
        resolve(wrapper());
      } catch (e) {
        reject(e);
      }
    }),
};
