location VANYAS_CASTLE = $location[Vanya's Castle];
location MEGALO_CITY = $location[Megalo-City];
location HEROS_FIELD = $location[Hero's Field];
location FUNGUS_PLAINS = $location[The Fungus Plains];

void loop8(location loc, string color)
{
    int base = 0;
    boolean bonus = false;
    switch (loc) {
    case VANYAS_CASTLE:
	base = 300;
	bonus = color == "black";
	break;
    case MEGALO_CITY:
	base = 300;
	bonus = color == "blue";
	break;
    case HEROS_FIELD:
	base = 100;
	bonus = color == "green";
	break;
    case FUNGUS_PLAINS:
	base = 150;
	bonus = color == "red";
	break;
    }
    print(loc + " base=" + base + " color=" + color);
    print("--------------");

    // Modifier value
    int minX = 0;
    int maxX = base + 300;

    // Points
    int minY = bonus ? 100 : 50;
    int maxY = bonus ? 400 : 200;

    // So we can detect when Y value (points) increases
    int curX = minx;
    int curY = miny;

    void print_range(int low, int high, int y)
    {
	string lowx = low == 0 ? "000" : to_string(low);
	string highx = to_string(high);
	print(lowx + "-" + highx + " -> " + y);
    }

    for (int x = minX; x <= maxX; ++x) {
	int points = eight_bit_points(loc, color, x);
	if (points < curY) abort(x + " -> " + points);
	if (points > curY) {
	    print_range(curX, x-1, curY);
	    curX = x;
	    curY = points;
	}
    }
    if (curX < maxX) {
	print_range(curX, maxX, curY);
    }
    print();
}

void main()
{
    void loop8(location loc, string... colors)
    {
	foreach i, color in colors {
	    loop8(loc, color);
	}
    }

    loop8(VANYAS_CASTLE, "white", "black");
    loop8(MEGALO_CITY, "white", "blue");
    loop8(HEROS_FIELD, "white", "green");
    loop8(FUNGUS_PLAINS, "white", "red");
}
