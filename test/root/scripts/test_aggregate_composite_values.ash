int[int] map1;
int[int][int] map2;

record test
{
    int n;
};

test[int] map3;

print(map1);
print("count(map1) = " + count(map1));
print("map1[10] = " + map1[10]);
print("count(map1) = " + count(map1));
print("map1 contains 10 = " + (map1 contains 10));
print();

print(map2);
print("count(map2) = " + count(map2));
print("map2[10] = " + map2[10]);
print("count(map2) = " + count(map2));
print("map2 contains 10 = " + (map2 contains 10));
print("map2[20][5] = " + map2[20][5]);
print("count(map2) = " + count(map2));
print("map2 contains 20 = " + (map2 contains 20));
print("count(map2[20]) = " + count(map2[20]));
print("map2[20] contains 5 = " + (map2[20] contains 5));
int[int] map2a = map2[30];
print("map2a = " + map2a);
print("count(map2) = " + count(map2));
print("map2 contains 30 = " + (map2 contains 30));
print("count(map2a) = " + count(map2a));
print("map2a[12]++ = " + map2a[12]++);
print("count(map2a) = " + count(map2a));
print("map2a contains 12 = " + (map2a contains 12));
print("map2a[12] = " + map2a[12]);
print("map2[30] contains 12 = " + (map2[30] contains 12));
print("map2[30][12] = " + map2[30][12]);
print();

print(map3);
print("count(map3) = " + count(map3));
print("map3[1] = " + map3[1]);
print("count(map3) = " + count(map3));
print("map3 contains 1 = " + (map3 contains 1));

print("map3[2].n = " + map3[2].n);
print("count(map3) = " + count(map3));
print("map3 contains 2 = " + (map3 contains 2));

print("map3[3].n++ = " + map3[3].n++);
print("count(map3) = " + count(map3));
print("map3 contains 3 = " + (map3 contains 3));
print("map3[3].n = " + map3[3].n);
