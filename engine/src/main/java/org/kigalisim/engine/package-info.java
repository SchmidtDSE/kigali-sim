/**
 * <strong>Kigali Sim Engine</strong>
 *
 * <p>This package contains the engine which actually performs simulation calculations.
 *
 * <p>At its core, there is some set of <strong>state</strong> describing a country in {@code
 * org.kigalisim.engine.state}. These values are often expressed as {@code EngineNumber}
 * instances whose units can be converted via {@code UnitConverter} (see {@code
 * org.kigalisim.engine.number}). The engine refers to "timesteps" in case of future changes which
 * support alternative granularity but, for now, timesteps are years in reflection of data
 * availability in most countries.
 *
 * <p>State <strong>changes</strong> can be performed directly by the user through commands like
 * {@code set} in QubecTalk. However, when numbers are modified, those new values are "propagated"
 * across related fields. This is done through stock flow modeling where, for example, a change in
 * sales may change the total amount of equipment in the country. This is performed through a
 * {@code RecalcKit} and {@code RecalcOperation} instances: strategies for performing these
 * updates. This logic can be found in {@code org.kigalisim.engine.recalc}.
 *
 * <p>After this pattern of setting values and propagating calculations, the results are
 * <strong>serialized</strong> to an {@code EngineResult} which serves as a snapshot of a
 * timestep. Typically this is converted eventually to a CSV file. However, depending on the
 * execution modality, this may happen through different pathways. See {@code
 * org.kigalisim.engine.serializer} for more details.
 *
 * @license BSD-3-Clause
 */
package org.kigalisim.engine;
