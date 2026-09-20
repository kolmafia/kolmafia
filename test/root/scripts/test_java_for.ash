for (int[3] x = int[] {1, 2}; ;) {
    dump(x);
    break;
}

for (float x = 0.0; x < 3.0; x += 1.0) {
    print(x);
}

for (string s = "a"; length(s) < 3; s += "b") {
    print(s);
}