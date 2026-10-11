Follow an existing one end-to-end (e.g. `BANISHING_SHOUT`, `PUNCH_OUT_YOUR_FOE`):

1. `src/main/resources/data/classskills.txt` — source of skill id/name/image/tags (e.g. skill 253 "Order a Kneecapping" lives here)
2. `SkillPool.java` — add `public static final int NAME = <skillId>`
3. `BanishManager.java` — add a `Banisher` enum entry `NAME("display name", duration, queueSize, isTurnFree, resetType)`; use `Reset.ROLLOVER_RESET` for "until rollover" banishes, `duration = -1` unless turn-based; `isTurnFree = false` unless the wiki mentions the skill does not take a turn
4. `FightRequest.payActionCost` — add `case SkillPool.NAME` matching a distinctive `responseText` substring, calling `BanishManager.banishMonster(monster, Banisher.NAME)`; set `skillSuccess` / `skillRunawaySuccess = true` (also drives the dailylimits increment below)
5. Daily limit (N/day): add `Cast\t<skill>\t<pref>\tN` to `src/main/resources/data/dailylimits.txt` and `user\t<pref>\t0` to `src/main/resources/data/defaults.txt`
6. Test: fixture in `src/test/root/request/test_fight_*.html` + `FightRequestTest` case via `parseCombatData(..., "fight.php?action=skill&whichskill=<id>")`

Note: wiki "When Used" text may be a placeholder ("This skill needs its in-game message"); treat it as a starting point and verify the matched substring against a real combat log. Also, try to avoid parts of the string that may change against plural monsters, or monsters with different pronouns (e.g. he / she / it).
