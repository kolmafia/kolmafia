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
