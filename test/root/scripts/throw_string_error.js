function fail() {
  throw "my error";
}

function wrapper() {
  fail();
}

wrapper();
