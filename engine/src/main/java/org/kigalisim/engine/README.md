# Kigali Sim Engine

This package contains the engine which actually performs simulation calculations.

At its core, there is some set of **state** describing a country in `org.kigalisim.state`. These values are often expressed as `EngineNumber` instances whose units can be converted via `UnitConverter` (see `org.kigalisim.number`). The engine refers to "timesteps" in case of future changes which support alternative granularity but, for now, timesteps are years in reflection of data availability in most countries.

State **changes** can be performed directly by the user through commands like `set` in QubecTalk. However, when numbers are modified, those new values are "propogated" across related fields. This is done through stock flow modeling where, for example, a change in sales may change the total amount of equipment in the country. This is performed through a `RecalcKit` and `RecalcOperation` instances: strategies for performing these updates. This logic can be found in `org.kigalisim.recalc`.

After this pattern of setting values and propogating calculations, the results are **serialized** to an `EngineResult` which serves as a snapshot of a timestep. Typically this is converted eventually to a CSV file. However, depending on the execution modality, this may happen through different pathways. See `org.kigalisim.serializer` for more details.
