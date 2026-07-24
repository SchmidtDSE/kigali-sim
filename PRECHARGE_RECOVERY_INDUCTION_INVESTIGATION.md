# Precharge + recovery/induction population shrinkage

**Status: fixed.** See "Fix (attempt 3 — the one that worked)" below. Full
suite (1166 tests) passes, including
`testPrechargeWithRecoveryInductionDoesNotShrinkPopulation`, and the fixed
behavior is a smooth, monotonically-growing induction effect relative to
BAU (not a boundary-scrape pass) — see that section for the actual
year-by-year numbers.

## Summary

With `recharge ... of newEquipment` (precharge) configured on a unit-tracked
substance, adding a `recover` policy with non-zero induction can *decrease*
total equipment population relative to BAU. Induction is supposed to do the
opposite: a non-zero induction rate should never make population lower than
the no-recycling baseline, since at minimum it displaces virgin material
1-for-1 (no population change) and at most it adds new demand on top
(population increase).

The same policy, using the same numeric rate on ordinary
`recharge ... of priorEquipment` instead of precharge, does not exhibit the
bug — population correctly increases relative to BAU. This was confirmed
directly: 100% recharge of priorEquipment with the same recovery/induction
policy produces `With Recycling` population *above* BAU every year; 100%
recharge of newEquipment (precharge) produces population *below* BAU
starting year 2. The defect is specific to precharge's circular dependency
on the value being solved for, not a general recharge-magnitude effect.

## Repro

`examples/precharge_recycling_induction.qta` (added alongside the
regression test below):

```
start default
  define application "Test"
    uses substance "SubA"
      enable import
      initial charge with 0 kg / unit for domestic
      initial charge with 1 kg / unit for import
      initial charge with 0 kg / unit for export
      equals 1000 kgCO2e / kg
      equals 1 kwh / unit
      set sales to 100 units during year 1
      recharge 100 % of newEquipment with 1 kg / unit
      recharge 5 % of priorEquipment with 1 kg / unit
    end substance
  end application
end default

start policy "Recycling"
  modify application "Test"
    modify substance "SubA"
      recover 50 % with 100 % reuse with 50 % induction
    end substance
  end application
end policy

start simulations
  simulate "BAU" from years 1 to 10
  simulate "With Recycling" using "Recycling" from years 1 to 10
end simulations
```

Regression test: `PrechargeLiveTests.testPrechargeWithRecoveryInductionDoesNotShrinkPopulation`
(currently failing by design — this documents the bug, not yet the fix).
Year 2: `BAU=200.0`, `With Recycling=196.25` (expected `>= 200.0`).

## Root cause

There are three places in the engine that each independently reason about
"how much recharge/precharge kg is embedded in a given sales figure":

1. **`SingleThreadEngine.recharge()` → `StreamUpdateExecutor.handleImplicitRecharge`
   → `ImplicitRechargeUpdateBuilder`** — used whenever a stream is set to a
   value in equipment units (e.g. the yearly persisted "100 units" sales
   target). Given N units, it *adds* recharge and precharge kg on top,
   computing precharge as `N * prechargeRatio * prechargeIntensity`.
2. **`SalesRecalcStrategy` / `DemandAnalysisBuilder` / `PrechargeVolumeCalculator`**
   — used by every `recharge`, `retire`, and `recover` statement's own
   per-tick recalculation. Independently computes a "required kg" total
   (`DemandAnalysis.getRequiredVirginMaterial()`) that already includes
   recharge, precharge, and induced-recycling kg, netting out what it
   believes is already embedded via the `implicitRecharge`/`implicitPrecharge`
   streams.
3. **`PopulationChangeRecalcStrategy` / `ServicingOffsetBuilder`** — the one
   place with a mathematically correct closed-form solve for the circular
   case: `newUnits = (salesKg - rechargeKg) / (initialCharge + prechargeRatio
   * prechargeIntensity)`.

`SalesRecalcStrategy.updateSalesStreams()` takes its fully-resolved kg total
from path 2 and converts it to "units" the naive way — dividing by initial
charge alone (the same defect already fixed for `LimitExecutor` in
`b981b758`/`440c4705`, but unconditional here rather than gated to
cap/floor). That inflates the implied unit count whenever recharge/precharge
kg rides along in the total. It then hands that inflated unit count to
`executeStreamUpdate`, which routes into path 1's machinery — which
**recomputes** recharge and precharge from that unit count and adds them
again.

Traced concretely for year 2 of the repro: a 96.25 kg requirement (already
correctly resolved by `DemandAnalysisBuilder`, including the induced
recycling contribution) gets converted to "96.25 units," which
`ImplicitRechargeUpdateBuilder` then re-expands to
`96.25 (initial charge) + 5 (recharge) + 96.25 (precharge, since precharge
ratio is 100%)` ≈ 197.5 kg — nearly double what was intended. This corrupted,
inflated kg then feeds `sales` (= domestic + import + recycle) into path 3's
otherwise-correct closed form. That closed form divides by the right
denominator (`initialCharge + prechargeRatio * prechargeIntensity`) but on
the now-wrong numerator, and the specific algebra of that combination lands
*below* the true answer for this configuration — hence the shrinkage rather
than growth.

**Why ordinary recharge-of-priorEquipment doesn't trigger this:** its
magnitude is anchored to the `priorEquipment` stream, fixed by the prior
cycle's outcome — entirely independent of the units value currently being
solved for. Double-counting it just adds a bounded, non-scaling error each
cycle. Precharge, by contrast, is defined as a ratio *of the very quantity
being solved for*, so any inconsistency between path 1 and path 2's
respective assumptions about "how much precharge is already embedded" gets
multiplied through the round trip, not just added.

## Attempted fixes (both reverted — documented so we don't repeat them)

### Attempt 1: invert the whole `updateSalesStreams` kg-to-units conversion

Make the kg-to-units conversion inside `SalesRecalcStrategy.updateSalesStreams()`
use the same closed-form inversion as `ServicingOffsetBuilder`:

```
units = (streamKg - rechargeShare) / (initialCharge + prechargeRatio * prechargeIntensity)
```

instead of naive `kg / initialCharge`, applied to the *entire* required-kg
figure (baseline + induced).

**Result: broke year 1**, which has no induction effect yet (`recover` has
nothing to recycle before any recharge volume exists) and previously matched
BAU exactly. `With Recycling` dropped from the correct 100 to 75.

**Why it failed:** `DemandAnalysisBuilder.calculateTotalDemand()` already
nets precharge out of the *baseline* portion of required kg via the
`implicitPrechargeKg` subtraction — that is the carry-over bookkeeping from
the previous cycle's persisted value, and in the simple case it cancels the
freshly-computed precharge exactly, leaving a baseline figure that only
needed the plain `/initialCharge` division all along. Dividing the *entire*
figure by `(initialCharge + prechargeRatio * prechargeIntensity)` double-
corrected that already-netted baseline. Only the **induced-recycling**
portion (`inducedDemandKg` inside `calculateRequiredVirginMaterialUnitsBased`)
is not covered by the implicit-tracking mechanism and actually needs some
form of correction.

### Attempt 2: rescale only `inducedDemandKg`

Localized the correction to `DemandAnalysisBuilder.calculateRequiredVirginMaterialUnitsBased`:
convert `inducedDemandKg` into "extra units" via
`inducedDemandKg / (initialCharge + prechargeRatio * prechargeIntensity)`,
then re-express that as pseudo-kg at rate `initialCharge` so the existing
(unchanged) downstream `/initialCharge` conversion recovers the right unit
count, leaving the baseline `totalDemand` handling untouched.

**Result: year 1 no longer regressed** (matches attempt-0 baseline, 26/27
tests pass), but year 2 moved in the **wrong direction** — `With Recycling`
dropped from 196.25 (pre-fix) to 195.625, further from the BAU=200 target,
not closer.

**Why it failed (best current understanding):** BAU itself is "self-healing"
— `PopulationChangeRecalcStrategy`'s closed form
(`deltaUnits = (salesKg - rechargeKg) / (initialCharge + prechargeRatio *
prechargeIntensity)`) always recovers the exactly-correct target population
(confirmed: BAU `popNew` is pegged at 100/year every year in the repro,
regardless of how messy the intermediate `import` kg bookkeeping gets
upstream), *as long as* whatever ends up in `salesKg` is internally
consistent with what `rechargeKg` expects. Given that, the marginal effect
of adding `extraKg` of induced demand to the final `sales` total is exactly
`extraKg / (initialCharge + prechargeRatio * prechargeIntensity)` more
population — supplied by that closed form directly, with no need to
pre-divide anything ourselves. Rescaling `inducedDemandKg` *before* it enters
the lossy `updateSalesStreams` → `ImplicitRechargeUpdateBuilder` round trip
does not compose the way attempt 2 assumed; the round trip does not simply
preserve whatever kg value flows into it, so shrinking the input beforehand
just shrinks the (already too-small) output further.

## The actual bug (found after attempts 1 and 2): a cross-call bookkeeping mismatch

Re-tracing the same year-2 numbers with fresh eyes surfaced the real defect,
which is narrower and more specific than either attempt above assumed.
There are (at least) two `SalesRecalcStrategy` invocations per year for this
substance: one triggered by `recharge 5% of priorEquipment` (call **A**),
and one triggered by `recover` (call **B**, later in the same tick).

- **Call A** computes `requiredVirginMaterial = 105` (target 100 units'
  worth of initial charge + 5 kg ordinary recharge; precharge nets to zero
  in this call because the freshly-computed precharge exactly cancels what
  was already embedded from the prior step). It converts this to "units"
  the naive way: `105 kg / 1 kg-per-unit = 105 units` — already wrong, since
  the true figure is 100 (the 5 kg of recharge riding on top is not
  unit-scaled and should not have been divided in at all). Setting the
  stream to "105 units" makes `ImplicitRechargeUpdateBuilder` record
  `implicitPrecharge = 105` (105 units × 100% × 1 kg/unit) — contaminated
  by the overcount.
- **Call B** (recover) runs next. It independently computes a *fresh*
  precharge estimate via `PrechargeVolumeCalculator`, which reads the
  `newEquipment` stream directly. That stream still holds the *correct*
  value, 100, because no actual population recalculation has happened yet
  this tick (call A's update used `propagateChanges(false)`).
- `DemandAnalysisBuilder.calculateTotalDemand()` nets these two "how much
  precharge is already embedded" estimates against each other
  (`totalDemand = ... - implicitPrechargeKg`), expecting fresh and implicit
  to match and cancel. They don't: fresh = 100, implicit = 105. That 5-unit
  gap becomes a **-5 kg residual baked straight into `totalDemand`** —
  exactly matching the observed number (`totalDemand = 95`, not the
  expected 100).

Ordinary recharge never has this problem because both its "fresh"
(`rechargeVolume`, computed from `priorEquipment`) and "implicit"
(`implicitRecharge`, recorded by whichever call ran last) estimates are
anchored to the same independent, already-settled `priorEquipment` stream —
neither depends on any call's own unit-conversion arithmetic, so they always
agree. Precharge's "fresh" estimate depends on `newEquipment` (correct, but
stale until an actual population recalc runs), while its "implicit" estimate
depends on whatever unit figure the *most recent* call happened to compute —
inheriting that call's naive-conversion error. The two attempts above didn't
target this because attempt 1 tried to fix the conversion formula globally
(wrong on baseline figures that don't have this problem) and attempt 2 only
touched the induced-demand term (a real but much smaller, separate issue —
still open, see caveats).

## Fix (attempt 3 — the one that worked)

`SalesRecalcStrategy.updateSalesStreams()`'s kg-to-units conversion now
subtracts the **residual** recharge — the portion of `rechargeVolume` not
already netted against `implicitRechargeKg` by `DemandAnalysisBuilder`
(`rechargeResidualKg = rechargeVolume - implicitRechargeKg`) — before
dividing by `initialCharge` alone (not `initialCharge + prechargeRatio *
prechargeIntensity`; that combined-denominator approach was attempt 1's
mistake).

The residual, not the raw volume, matters: an earlier version of this fix
subtracted the *full* `rechargeVolume` unconditionally and it had **zero net
effect** on the test's outcome — call A gained the correct +5 kg fix (its
`implicitRechargeKg` going in was 0, so residual = full volume there), but
call B's *own* recharge was already fully netted by that point
(`implicitRechargeKg` going into call B was already 5, matching its fresh
`rechargeVolume` of 5, so residual = 0) — over-subtracting it there
introduced a new -5 kg error that exactly canceled the fix, landing on the
identical wrong answer (196.25) as no fix at all. Passing
`implicitRechargeKg` through and subtracting only the true residual fixed
both calls correctly.

**Verified result:** all 1166 engine tests pass, and the fixed behavior for
the repro is not a boundary-scrape — population grows smoothly and
monotonically relative to BAU as the recharge base compounds:

| year | BAU | With Recycling | delta |
|------|-----|-----------------|-------|
| 1 | 100 | 100.00 | 0 |
| 2 | 200 | 201.25 | 1.25 |
| 3 | 300 | 303.77 | 3.77 |
| 4 | 400 | 407.56 | 7.56 |
| 5 | 500 | 512.66 | 12.66 |
| 10 | 1000 | 1058.17 | 58.17 |

As a cross-check: year 2's delta (1.25) matches *exactly* the delta measured
earlier for the ordinary-recharge-only (no precharge) comparison case,
supporting that this is the correct fix rather than a coincidental
cancellation.

## Caveats / open questions

- Attempt 2's concern (the induced-recycling term itself may still need
  `(c + p)`-aware treatment, since an induced *new* unit should also fund
  its own precharge) has not been separately re-verified now that the
  larger cross-call residual is gone. The full suite passes and the repro's
  numbers look smooth and physically sensible, so if a residual induction-
  scaling error remains, it's small enough not to violate the
  `>= BAU` regression check across 10 years — but it hasn't been proven
  absent, only not currently visible.
- This does not unify all three subsystems' understanding of "current new
  equipment count" — `LimitExecutor` was already handled separately in
  `b981b758`/`440c4705`, and interacts with this fix only in that both now
  correctly account for recharge/precharge riding on top of a kg total
  before converting to units, via different (independently-arrived-at)
  mechanisms. Worth a future pass to see if they can share logic.
- Worth double-checking behavior when `recover` targets EOL rather than
  RECHARGE stage, and when precharge is configured as an absolute (not
  percentage) population, since `ServicingOffsetBuilder` has a separate
  branch (`offsetVolumeSalesExplicitPrecharge`) for that case. This fix only
  touches the ordinary-recharge residual, not precharge's own residual, so
  the absolute-precharge case was not specifically exercised.
