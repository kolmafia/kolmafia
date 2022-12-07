void process(coinmaster cm, string token) {
	if (token != "") {
		string out = to_string(cm) + " uses " + token;
		item tokenItem = to_item(token);
		if (tokenItem == $item[none]) {
			out = to_string(cm) + " uses " + token + " which is not an item.";
			print(out);
		}		
	}
}
foreach cm in $coinmasters[]
{
	string token = cm.token;
	process(cm, token);
	string clist = cm.multiple_tokens;
  	string[] tokens = split_string(clist, ",");
	foreach it in tokens {
		process(cm, tokens[it]);
	}
}
print("Coinmaster checks all done.")
