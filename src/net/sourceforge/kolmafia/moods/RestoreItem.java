package net.sourceforge.kolmafia.moods;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

public abstract class RestoreItem implements Comparable<RestoreItem> {
  protected final String restoreName;
  protected final AdventureResult itemUsed;
  protected final int spleenHit;
  protected final int skillId;

  public RestoreItem(final String restoreName) {
    this.restoreName = restoreName;

    if (ItemDatabase.contains(restoreName)) {
      this.itemUsed = ItemPool.get(restoreName, 1);
      this.spleenHit = ConsumablesDatabase.getSpleenHit(restoreName);
      this.skillId = -1;
    } else if (SkillDatabase.contains(restoreName)) {
      this.itemUsed = null;
      this.skillId = SkillDatabase.getSkillId(restoreName);
      this.spleenHit = 0;
    } else {
      this.itemUsed = null;
      this.skillId = -1;
      this.spleenHit = 0;
    }
  }

  public boolean isSkill() {
    return this.skillId != -1;
  }

  public AdventureResult getItem() {
    return this.itemUsed;
  }

  public abstract boolean usableInCurrentPath();

  public abstract void recover(final int needed, final boolean purchase);

  // This will likely be overridden
  @Override
  public int compareTo(final RestoreItem o) {
    return this.restoreName.compareTo(o.restoreName);
  }

  @Override
  public String toString() {
    return this.restoreName;
  }
}
