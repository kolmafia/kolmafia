record rec1
{
    int x;
    int y;
};

record rec2
{
    int x;
    int y;
};

rec1 var1 = new rec1(10, 20);
rec1 var2 = new rec2(30, 40);

print("rec1 -> rec1: x = " + var1.x + " y = " + var1.y);
print("rec2 -> rec1: x = " + var2.x + " y = " + var2.y);
