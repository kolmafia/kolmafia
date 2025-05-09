package net.sourceforge.kolmafia.moods;

import net.sourceforge.kolmafia.persistence.SkillDatabase;

public class ManaBurn implements Comparable<ManaBurn> {
  private final int skillId;
  private final String skillName;
  private final String effectName;

  private int duration;
  private final int limit;
  private int count;

  public ManaBurn(
      final int skillId,
      final String skillName,
      final String effectName,
      final int duration,
      final int limit) {
    this.skillId = skillId;
    this.skillName = skillName;
    this.effectName = effectName;
    this.duration = duration;
    this.limit = limit;
    this.count = 0;
  }

  public boolean isCastable(long allowedMP) {
    if (this.duration >= this.limit) {
      return false;
    }

    // The max(1,...) guarantees that this loop will terminate.

    long cost = Math.max(1, this.getMPCost());

    return cost <= allowedMP;
  }

  public long simulateCast() {
    ++this.count;
    this.duration += SkillDatabase.getEffectDuration(this.skillId);

    return this.getMPCost();
  }

  @Override
  public int compareTo(final ManaBurn o) {
    return this.duration - o.duration;
  }

  private long getMPCost() {
    return SkillDatabase.getMPConsumptionById(this.skillId);
  }

  @Override
  public String toString() {
    return "cast " + this.count + " " + this.skillName + " ^ " + this.effectName;
  }
}
