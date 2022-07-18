// Mathematical
print(1 + 2);
print(1.0 + 0.5);
print("s" + "t");

print(1 - 2);
print(2.1 - 0.3);

print(2 * 2);
print(0.5 * 4);

print(4 / 3);
print(1 / 0.5);

print(11 % 7);
print(5.0 % 2.0);

print(2 ** 3);
print(4 ** 0.5);

print();

// Bitwise
print(1 & 3);
print(1 | 3);
print(1 ^ 3);
print(~false);
print(2 << 1);
print(2 >> 1);
print(2 >>> 1);

print();

// Assignment
int x = 1;
print(x += 1);
print(x);
print(x -= 1);
print(x);
print(x *= 10);
print(x);
print(x /= 2);
print(x);
print(x %= 3);
print(x);
print(x **= 3);
print(x);
print(x <<= 2);
print(x);
print(x >>= 1);
print(x);
print(x >>>= 1);
print(x);
print(x++);
print(x);
print(++x);
print(x);
print(--x);
print(x);
print(x--);
print(x);
boolean y = true;
print(y);
print(y &= false);
print(y);
print(y |= true);
print(y);
print(y ^= true);
print(y);

print();

// Relational
print(1 == 1);
print(1 == 1.0);
print(1 == 1.5);
print(1 != 1);
print(1 != 1.0);
print(1 != 1.5);
print(1 > 2);
print(2 < 3);
print(1 < 1);
print(1 <= 1);
print(1 >= 1);
print("a" < "b");
print("avec" == "AVEC");
print("avec" ≈ "AVEC");
print("avec" ≈ "ACEV");

print();

// Boolean
print(!false);
print(!true);

boolean print_something(int x) {
	print("I was called with " + x);
	return true;
}

print(false && print_something(1));
print(true && print_something(2));
print(false || print_something(3));
print(true || print_something(4));
