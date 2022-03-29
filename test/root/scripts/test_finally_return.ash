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
    }
    print("Good enough!");
    return input;
}

print("I received " + test(50));
print("I received " + test(100));