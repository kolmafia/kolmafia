int totalItems = 0;
int totalDisplay = 0;
int totalGift = 0;
int totalTrade = 0;
	
foreach it in $items[]
	{
		totalItems++;
		if (is_displayable(it))	totalDisplay++;
		if (is_giftable(it))	totalGift++;
		if (is_tradeable(it))	totalTrade++;
	}
print("Total tradeable items: " + totalTrade);
print("Total giftable items: " + totalGift);
print("Total displayable items: " + totalDisplay);
print("Total items: " + totalItems);
