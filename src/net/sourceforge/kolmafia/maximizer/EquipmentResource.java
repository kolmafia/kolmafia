package net.sourceforge.kolmafia.maximizer;

import java.util.Map;
import java.util.function.ToIntFunction;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.equipment.Slot;

record EquipmentResource(
    String name,
    int limit,
    ToIntFunction<Map<Slot, AdventureResult>> equipmentCost,
    ToIntFunction<String> itemCost) {}
