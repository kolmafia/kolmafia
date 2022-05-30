int[int][int] map1;
int[int] map2 = map1[10];
map2[5] = 12;
print(map1[10][5]);
record test {int x;};
test[int] map;
test rec = map[5];
rec.x = 100;
print(map[5].x);