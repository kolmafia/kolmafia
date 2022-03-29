int test( int input )
{
    try {
        print("You gave me " + input);
    } finally {
        if (input == 100) {
            int output = input + 1;
            print("I prefer " + output);
            return output;
        }
	if (input == 200) {
	    abort("No way!");
	}
    }
    print("Good enough!");
    return input;
}

print("I received " + test(50));
print("I received " + test(100));
print("I received " + test(200));