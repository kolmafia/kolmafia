item st = $item[seal tooth];
item sh = $item[seal-skull helmet];

print(st);
print(sh);
print("equals = " + (st == sh));
print("-------");

record rec
{
    item it;
    int count;
};

string to_string(rec r)
{
    return r.it + " (" + r.count + ")";
}

rec zero = new rec(st, 0);
rec one = new rec(st, 1);
rec two = new rec(st, 1);
rec three = new rec(sh, 1);

print("zero = " + zero);
print("one = " + one);
print("two = " + two);
print("three = " + three);
print("zero == one: " + (zero == one));
print("one == one: " + (one == one));
print("one == two: " + (one == two));
print("one == three: " + (one == three));
print("-------");

record strec {
    string str;
};

strec strec1 = new strec("ABC");
strec strec2 = new strec("abc");

print("strec1.str = " + strec1.str);
print("strec2.str = " + strec2.str);
print("strec1 == strec2: " + (strec1 == strec2));
print("strec1 ≈ strec2: " + (strec1 ≈ strec2));
print("-------");

typedef int[] int_array;

string to_string(int_array ia)
{
    buffer buf;
    buf.append("[");
    foreach index, val in ia {
	if (buf.length() > 1) {
	    buf.append(",");
	}
	buf.append(val);
    }
    buf.append("]");
    return buf;
}

record iarec {
    int_array ia;
};

int_array ia1 = {1, 2, 3};
iarec iarec1 = { ia: ia1 };
iarec iarec2 = { ia: ia1 };
iarec iarec3 = { ia: {1, 2, 3} };
iarec iarec4 = { ia: {1, 2, 3, 4 } };

print("iarec1.ia = " + iarec1.ia);
print("iarec2.ia = " + iarec2.ia);
print("iarec3.ia = " + iarec3.ia);
print("iarec4.ia = " + iarec4.ia);
print("iarec1 == iarec1: " + (iarec1 == iarec1));
print("iarec1 == iarec2: " + (iarec1 == iarec2));
print("iarec1 == iarec3: " + (iarec1 == iarec3));
print("iarec1 == iarec4: " + (iarec1 == iarec4));
