var i = 1;
print(i);
print(i == 1);

var f = 1.0;
print(f);
print(f == 1.0);

var s = "str";
print(s);
print(s == "str");

var b = true;
print(b);

var a = 1, c = 2;
print(a);
print(c);

static var sv = 10;
print(sv);

int[] arr = {1, 2, 3};
var agg = arr;
print(count(agg));
print(agg[2]);

var it = $item[seal tooth];
print(it);

var st = get_stack_trace();
print(count(st));

record Point
{
    int x;
    int y;
};

Point make_point(int x, int y) {
    return new Point(x, y);
}

var p = make_point(7, 9);
print(p.x + p.y);