const {print} = require("kolmafia");
new Promise((resolve, reject) => reject("bar"))
print("foo");
