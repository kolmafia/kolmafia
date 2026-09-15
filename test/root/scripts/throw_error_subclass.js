// How TypeScript emits "class MyError extends Error" for older targets: the thrown value is a real
// Error whose prototype has been swapped for a plain object, which Rhino cannot read .stack off.
function MyError(message) {
  var self = Error.call(this, message) || this;
  Object.setPrototypeOf(self, MyError.prototype);
  return self;
}
MyError.prototype = Object.create(Error.prototype);

function fail() {
  throw new MyError("subclassed failure");
}

fail();
