boolean ok;
ok = ($class[Avatar of Sneaky Pete] == $class[Avatar of Sneaky Pete]);
print("comparison: " + to_string(ok));
ok = ($class[Gelatinous Noob].primestat == $stat[Moxie]);
print("primestat: " + to_string(ok));
ok = ($class[Ed] == $class[Ed the Undying]);
print("substring matching: " + ok);
ok = ("astral spirit".to_class() == $class[none]);
print("no astral spirit: " + ok);
