const { dump, beretBuskingEffects } = require("kolmafia");
// Before the soft cap
dump(beretBuskingEffects(85, 0));
dump(beretBuskingEffects(85, 1));
dump(beretBuskingEffects(85, 2));
dump(beretBuskingEffects(85, 3));
dump(beretBuskingEffects(85, 4));

// After the soft cap
dump(beretBuskingEffects(3450, 0));
dump(beretBuskingEffects(3450, 1));

// Duplicate
dump(beretBuskingEffects(295, 2));
