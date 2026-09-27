// snarfblat locations have an id
print("Snarfblat location has an id: " + ($location[Noob Cave].id == 240));
print("Snarfblat location can be converted to int: " + ($location[The Dire Warren].to_int() == 92));

// non snarfblat locations
print("Non-snarfblat locationas have id of -1: " + ($location[Summoning Chamber].id == -1));
print("Non-snarfblat locationas have int value of -1: " + ($location[The Lower Chambers].to_int() == -1));

// can adventure
print("Location none cannot be adventured in: " + ($location[none].can_adventure() == false));