const {print} = require("kolmafia");

module.exports = {
  main: () => {
    new Promise((resolve, reject) => reject("bar")).then(
        (value) => print(value),
        (err) => {
          print("Error: " + err)
          return "foo";
        }
    )
    print("foo")
  }
};
