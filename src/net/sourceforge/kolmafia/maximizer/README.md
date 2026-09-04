# Modifier Maximizer internals

This package turns a user expression such as `item, 0.5 meat, 5 cold res min`
into equipment and effect recommendations. It deliberately separates the
expression language, candidate compilation, stateful search, scoring, and
recommendation rendering. A new game mechanic should normally change one of
those layers rather than add another special case to `Maximizer`.

## Data flow

```text
maximize expression
        |
        v
MaximizerExpressionParser ---> MaximizerTermRegistry
                                      |
                                      v
                                  Evaluator
                                      |
                 +--------------------+--------------------+
                 |                                         |
                 v                                         v
       EquipmentSearchRunner                     effect source strings
                 |                                         |
       +---------+----------+                              v
       |                    |                   EffectSourceDispatcher
       v                    v                              |
ordinary candidates   Codpiece candidates                 v
       |                    |                   EffectSourcePlanFinalizer
       +---------+----------+                              |
                 |                                         |
                 v                                         |
      CandidateLoadoutFactory                              |
                 |                                         |
                 v                                         |
      CandidateShortlistCompiler                           |
                 |                                         |
                 v                                         |
       EquipmentSearchProblem                              |
                 |                                         |
                 v                                         |
            AnytimeSearch                                  |
                 ^                                         |
                 | deadline and incumbent                   v
          MaximizerSession                         source recommendation
                 |                                         |
                 v                                         |
         best MaximizerLoadout                              |
                 |                                         |
                 +--------------------+--------------------+
                                      v
                                  Maximizer
                                      |
                                      v
                                  Boost list
```

`Maximizer` is the application-facing coordinator. It creates a session,
invokes the equipment pipeline, emits equipment changes, evaluates useful
effects, and publishes `Boost` rows to the GUI and scripting APIs. It is not
the expression parser, candidate compiler, scoring implementation, or search
algorithm.

## Main responsibilities

### Entry points and expression semantics

| File | Responsibility |
| --- | --- |
| `Maximizer.java` | Coordinates one maximize request and converts its equipment and effect results into executable `Boost` recommendations. |
| `MaximizerExpressionParser.java` | Tokenizes weights and keywords. It owns syntax only and delegates meaning to the registry. |
| `MaximizerTermRegistry.java` | Defines the query language and stores all values parsed from one expression. |
| `Evaluator.java` | Applies registry semantics to modifiers, constraints, and tiebreaking, then starts equipment compilation. |
| `EvaluationOutcome.java` | Immutable primary score, feasibility, and saturation result. |
| `EquipScope.java` | Defines which acquisition sources a maximize request may consider. |
| `PriceLevel.java` | Defines when mall prices must be consulted. |

### Candidate discovery and acquisition

| File | Responsibility |
| --- | --- |
| `CheckedItem.java` | Equipment candidate plus its compiled availability and search flags. |
| `ItemAvailability.java` | Immutable counts and ordered acquisition options for an item. |
| `ItemAvailabilityCompiler.java` | Computes accessible, craftable, foldable, pullable, NPC, and mall copies under the request scope. |
| `AcquisitionOption.java` | Acquisition method and quantity value types. |
| `OrdinaryCandidateCompiler.java` | Walks the equipment database and builds the full and ranked ordinary-item catalogs. |
| `OrdinaryCandidateEvaluator.java` | Rejects illegal or irrelevant items and marks required, conditional, and mechanically useful candidates. |
| `EquipmentCandidateSlotter.java` | Assigns an accepted item to the slot or auxiliary ranking bucket where it must compete. |
| `FamiliarEquipmentCompiler.java` | Compiles familiar-equipment candidates for the current and requested familiars. |
| `EquipmentSetEvaluator.java` | Retains complete outfit and synergy choices that isolated item scoring would otherwise discard. |
| `CardSleeveSelector.java` | Selects the card used when evaluating a card sleeve candidate. |
| `CarriedFamiliarSelector.java` | Selects Crown of Thrones and Buddy Bjorn occupants. |
| `ModeableSelector.java` | Selects useful states for modeable equipment. |
| `CandidateLoadoutFactory.java` | Builds comparable one-candidate loadouts used for ranking. |
| `CandidateShortlistCompiler.java` | Reduces ranked catalogs to the candidates needed by the interactive search while preserving required choices. |

### Search and evaluation

| File | Responsibility |
| --- | --- |
| `EquipmentSearchRunner.java` | Connects candidate compilation, special selectors, shortlist construction, and search execution. |
| `EquipmentSearchProblem.java` | Represents the reversible, phase-based equipment decision tree presented to `AnytimeSearch`. |
| `AnytimeSearch.java` | Domain-independent deterministic depth-first anytime branch-and-bound. |
| `MaximizerSession.java` | Owns one invocation's incumbent, deadline, limits, cancellation checks, progress, and metrics. |
| `MaximizerLoadout.java` | Mutable hypothetical equipment, familiar, mode, and effect state evaluated by the search. |
| `LoadoutEvaluation.java` | Caches a loadout's score, feasibility, resource usage, tiebreaker, and final quality. |
| `SolutionQuality.java` | Defines the authoritative lexicographic ordering between complete legal solutions. |
| `CharacterSnapshot.java` | Captures character facts that must stay stable during one search. |
| `ResourceUsage.java` | Tracks shared constrained resources such as Beeosity and compares lower usage favorably. |
| `SearchMetrics.java` | Immutable summary of candidate and search work from the last run. |
| `SlotList.java` | Slot-indexed lists, including virtual buckets for familiar candidates and ranking-only categories. |
| `MaximizerExceededException.java` | Stops search after reaching a requested global maximum. |
| `MaximizerLimitException.java` | Stops search after reaching the configured combination limit. |
| `MaximizerInterruptedException.java` | Stops search when the user or application cancels the request. |

### Slotted equipment and the Eternity Codpiece

| File | Responsibility |
| --- | --- |
| `SlottedItem.java` | Maximizer-local typed access to parent equipment whose modifiers depend on child-slot occupants. |
| `CodpieceEvaluator.java` | Discovers and ranks Codpiece gem candidates in isolation. |
| `CodpieceModifierSafety.java` | Conservatively decides which gem modifiers support cached incremental evaluation and safe bounds. |
| `CodpieceSearchState.java` | Owns canonical gem-multiset traversal, copy accounting, cached modifier prefixes, and suffix bounds for one loadout. |

The Codpiece needs more machinery than a card sleeve or folder holder because
its five interchangeable slots create a large search. With 73 possible gems
plus an empty choice, slot-by-slot enumeration reaches 74^5 (2,219,006,624)
assignments. Canonicalizing slot permutations still leaves 21,111,090
configurations of up to five gems before considering the surrounding equipment
choices. Gem effects can also depend on familiars, derived modifiers, caps, and
shared copy counts.

`CodpieceSearchState` therefore:

1. enumerates canonical multisets rather than slot permutations;
2. rejects selections that need more copies than are obtainable;
3. reuses the expensive character-adjustment prefix when every selected gem is
   safe for late application;
4. applies an optimistic suffix bound only for modifier semantics classified by
   `CodpieceModifierSafety`; and
5. falls back to full modifier calculation whenever that proof is unavailable.

The fallback is intentional. A slower exact evaluation is preferable to an
unsafe bound that could prune the correct answer.

### Recommendation rendering

| File | Responsibility |
| --- | --- |
| `Boost.java` | One displayable and optionally executable equipment, effect, familiar, or mode recommendation. |
| `EffectSourceDispatcher.java` | Maps a status-effect source command family to one named game-specific availability rule and a mutable source plan. |
| `EffectSourcePlanFinalizer.java` | Applies common item acquisition, price, resource, cost, and display rules after source-specific dispatch. |

The dispatcher is intentionally a `switch`, not a generic rules framework.
Command precedence is visible in one place, while each named handler contains
only the exceptional game rule. A handler either keeps the plan, disables its
command while retaining explanatory text, or skips it. The finalizer owns all
behavior common to every source.

## Extending the Maximizer

### Add or change an expression term

1. Decide whether the syntax is:
   - a **directive**, which changes constraints or search behavior;
   - a **score term**, which contributes to the numeric objective;
   - an **alias**, which maps alternate text to an existing action; or
   - a **rewrite**, which normalizes generic spelling before modifier lookup.
2. Register it in the static initialization block of
   `MaximizerTermRegistry`.
3. Store parsed state on the registry and expose only the query needed by
   `Evaluator` or candidate compilation.
4. Preserve resolution order in `MaximizerTermRegistry.apply`:
   directives, equipment slots, exact numeric modifiers, rewrites, boolean
   modifiers, registered score terms, then failure.
5. Remember that `min` and `max` apply to the last recognized numeric score
   modifier. Directives and slot keywords resolve before that association is
   reset, so slots do not clear it; other non-numeric terms do. A new directive
   must explicitly decide whether it preserves or clears that association.
6. Add parsing/state tests to `MaximizerTermRegistryTest` and scoring tests to
   `EvaluatorTest` or `MaximizerTest`.
7. Update `src/data/maximizer-help.html` for user-visible syntax.

Do not add another parser branch to `Evaluator`; parsing and registration belong
to the registry.

### Add an effect source command

1. Add the source string to the normal effect-source data.
2. Add its command family to `EffectSourceDispatcher.dispatch`.
3. Implement a named handler that populates `Plan`:
   - return `plan.skip()` when the source must not be shown;
   - call `plan.disable()` when it should be shown but cannot currently run;
   - call `plan.disable(text)` when the unavailable source needs explanatory
     text;
   - set `plan.item` when the common acquisition pipeline should retrieve an
     item; and
   - set only source-specific costs, duration, and remaining uses.
4. Do not duplicate inventory, mall-price, organ, resource, or display logic;
   `EffectSourcePlanFinalizer` owns it.
5. Match the full command prefix when a family name alone would accept invalid
   custom mood actions.
6. Add a focused `MaximizerTest` covering available, unavailable, `includeAll`,
   and exhausted-use behavior where applicable.

### Add ordinary equipment behavior

1. Put acquisition counting in `ItemAvailabilityCompiler`.
2. Put legality or usefulness in `OrdinaryCandidateEvaluator`.
3. Put slot placement in `EquipmentCandidateSlotter`.
4. Preserve interacting outfit or synergy pieces in `EquipmentSetEvaluator`.
5. Add a selector only when a child choice must be chosen before ordinary
   traversal, as with cards, modes, or carried familiars.
6. Change `EquipmentSearchProblem` only when the decision tree itself gains a
   new kind of phase or reversible choice.

Do not teach `AnytimeSearch` about items, slots, familiars, paths, or KoL
mechanics.

### Add a slotted parent item

1. Register the parent, child slots, modifier type, and occupant validation in
   `ItemSlotGroup` or `FamiliarSlotGroup`.
2. Use `get` and `put` for hypothetical `MaximizerLoadout` state rather than
   reading or mutating live equipment.
3. Mark an item-slot group searchable only when candidate selection exists;
   otherwise copy its current occupants when evaluating the parent.
4. If occupants require combinatorial search, first try the ordinary
   `EquipmentSearchProblem` phases. Add specialized state only when measured
   performance or non-additive semantics require it.
5. Any bound must be optimistic for every supported modifier. Unsupported
   semantics must fall back to exact evaluation.

### Change solution preference

`SolutionQuality.compareTo` is the single ordering contract. Its field order is
behavior, not presentation. Add or reorder a dimension only with focused
comparison tests and an explanation of why it belongs ahead of later
tiebreakers.

## Testing

- Parser and registry behavior: `MaximizerTermRegistryTest`
- Scoring and constraints: `EvaluatorTest`
- Candidate compilation: the corresponding compiler or selector test
- Search transitions and bounds: `EquipmentSearchProblemTest`
- End-to-end behavior: `MaximizerTest` and `MaximizerRegressionTest`
- Codpiece behavior and performance: `EternityCodpieceMaximizerTest`
- Independent exactness checks: reduced universes compared with
  `BruteForceMaximizer`

Run the narrowest relevant test first, then the full
`net.sourceforge.kolmafia.maximizer.*` suite for changes crossing these
boundaries.
