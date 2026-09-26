boolean ok;
ok = cli_execute("test cookies");
print("cookies " + to_string(ok));
ok = cli_execute("test itemids saucepan");
print("saucepan " + to_string(ok));
