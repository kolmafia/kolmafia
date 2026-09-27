const {print} = require("kolmafia");
new Promise((resolve, reject) => reject("bar")).then(
    (value) => print(value),
    (err) => {
      print("Error: " + err);
      return "baz";
    }
);
print("foo")