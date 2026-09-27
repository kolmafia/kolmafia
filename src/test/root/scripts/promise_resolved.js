const {print} = require("kolmafia");
new Promise((resolve) => resolve("foo")).then((value) => print(value));
