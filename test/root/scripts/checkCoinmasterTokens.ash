// Checks that every CoinMaster has a token, or a list of tokens but not both
foreach cm in $coinmasters[] {
	if (cm.token != "") continue;
	string list = cm.multiple_tokens;
	if (contains_text(list, ",")) continue;
	print("Problem with " + cm);
}
print("All Done");