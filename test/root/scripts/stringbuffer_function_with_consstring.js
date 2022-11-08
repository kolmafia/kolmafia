const {bufferToFile, fileToBuffer, print} = require("kolmafia");

const filename = "data/test_stringbuffer_function_with_consstring.txt"

const foo = "foo";
bufferToFile(foo + "bar", filename)
print(fileToBuffer(filename));