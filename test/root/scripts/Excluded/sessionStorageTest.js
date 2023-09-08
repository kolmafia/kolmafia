const { print, sessionStorage } = require("kolmafia");

print(sessionStorage.getItem("test") == null);