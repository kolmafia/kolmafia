const {print} = require("kolmafia");
module.exports = {
  main: () => {
    new Promise((resolve, reject) => reject("bar"))
    print("foo");
  }
};
