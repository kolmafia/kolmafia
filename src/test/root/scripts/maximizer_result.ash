record MaximizerResult
{
    string display;
    string command;
    float score;
    effect effect;
    item item;
    skill skill;
};

record MaximizerResultFull
{
    string display;
    string command;
    float score;
    effect effect;
    item item;
    skill skill;
    string afterdisplay;
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
    return new MaximizerResult("", "", 0.0, $effect[none], $item[none], $skill[none]);
}

MaximizerResultFull do_maximize_full(string command, int price, int priceLevel, int equipScope, string filter) {
    MaximizerResultFull[] results = maximize(command, price, priceLevel, equipScope, filter);
    int count = count(results);
    print(count + " results");
    MaximizerResultFull retval;
    if (count > 0) {
        print(results[0]);
        return results[0];
    }
    return new MaximizerResultFull("", "", 0.0, $effect[none], $item[none], $skill[none], "");
}

MaximizerResult result = do_maximize("fishing skill", 0, 0, true, false);
print("display = " + result.display);
print("command = " + result.command);
print("score = " + result.score);
print("effect = " + result.effect);
print("item = " + result.item);
print("skill = " + result.skill);

MaximizerResultFull resultFull = do_maximize_full("fishing skill", 0, 0, 0, "");
print("display = " + resultFull.display);
print("command = " + resultFull.command);
print("score = " + resultFull.score);
print("effect = " + resultFull.effect);
print("item = " + resultFull.item);
print("skill = " + resultFull.skill);
print("afterdisplay = " + resultFull.afterdisplay);
