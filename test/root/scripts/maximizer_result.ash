record MaximizerResult
{
    string display;
    string command;
    float score;
    effect effect;
    item item;
    skill skill;
};

MaximizerResult do_maximize(string command, int price, int priceLevel, boolean speculate, boolean equip) {
    MaximizerResult[] results = maximize(command, price, priceLevel, speculate, equip);
    int count = count(results);
    print(count + " results");
    MaximizerResult retval;
    if (count > 0) {
	print(results[0]);
	return results[0];
    }
    return new MaximizerResult("", "", 0.0, $effect[none], $item[ none], $skill[ none]);

}

MaximizerResult result = do_maximize("initiative", 0, 0, true, false);
print("display = " + result.display);
print("command = " + result.command);
print("score = " + result.score);
print("effect = " + result.effect);
print("item = " + result.item);
print("skill = " + result.skill);
