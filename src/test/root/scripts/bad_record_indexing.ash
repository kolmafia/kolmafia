record rec {
  int x;
};
rec a;
int ax1 = a.x;
int ax2 = a[0];
int ax3 = a[ax1];
rec b = new rec;
rec c = new rec(b.x);
rec d = new rec[1];
rec e = new rec[a.x];

print(rec[int]{1: new rec(1)}[3][0]);
print(rec[int]{
  2: new rec(2)
}[3][0]);

// Fine, but never printed, since the script is never evaluated.
print(rec[int]{0: new rec(0)}[3].x);
