record Point
{
    int x;
    int y;
};

Point printPoint(string name, Point p)
{
    print(name + ": x = " + p.x + " y = " + p.y);
    return p;
}

Point point1 = { x: 1, y : 2 };
Point point2 = new Point(13, 31);
Point point3 = { y: 4, x : 3 };

printPoint("point1", point1);
printPoint("point2", point2);
printPoint("point3", point3);

record PointArray
{
    string name;
    Point[] points;
};

PointArray printPointArray(PointArray pa)
{
    print(pa.name + " has " + count(pa.points) + " points");
    foreach n, p in pa.points {
	string id = pa.name + "[" + n + "]";
	printPoint(id, p);
    }
    return pa;
}

PointArray pa1 = {
 name : "pa1",
 points : {
     { x : 1, y : 2 },
     new Point(10, 20),
     { x : 100, y : 200 }
 }
};

printPointArray(pa1);

record PointMap
{
    string name;
    Point[int] points;
};

PointMap printPointMap(PointMap pm)
{
    print(pm.name + " has " + count(pm.points) + " points");
    foreach n, p in pm.points {
	string id = pm.name + "[" + n + "]";
	printPoint(id, p);
    }
    return pm;
}

PointMap pm1 = {
 name : "pm1",
 points : {
    1 : { x : 1, y : 2 },
    3 : new Point(10, 20),
    5 : { x : 100, y : 200 }
 }
};

printPointMap(pm1);