/**
 * Indexed, memoized wrapper over simulation results for fast repeated queries.
 *
 * @license BSD, see LICENSE.md.
 */

import {ReportDataWrapper} from "report_data";

/**
 * Single-dimension index over row indices, keyed by exact value.
 *
 * Also maintains a meta-group prefix index so "X - All" style filter values
 * (as used for applications and substances, see
 * ReportDataWrapper.stepWithSubtype) can be resolved via a lookup instead of
 * a scan. Building the meta-group index is a no-op for non-string keys (e.g.
 * year) and for strings with no " - " separator (e.g. plain scenario names),
 * so this class is safe to use uniformly across all indexed dimensions.
 */
class IndexAccumulator {
  /**
   * Create a new, empty index accumulator.
   */
  constructor() {
    const self = this;

    /**
     * Row indices keyed by their exact value.
     *
     * @private
     * @type {Map<*, Set<number>>}
     */
    self._exact = new Map();

    /**
     * Row indices keyed by meta-group prefix (the portion of a value up to
     * and including a " - " boundary).
     *
     * @private
     * @type {Map<string, Set<number>>}
     */
    self._metaGroupPrefix = new Map();
  }

  /**
   * Register a row under a value, indexing both its exact value and any
   * meta-group prefixes it implies.
   *
   * @param {*} key - The value to index (e.g. a scenario name or year).
   * @param {number} rowIndex - The row's index in the underlying rows array.
   */
  addToIndex(key, rowIndex) {
    const self = this;
    self._addToMap(self._exact, key, rowIndex);
    self._addMetaGroupPrefixes(key, rowIndex);
  }

  /**
   * Get the distinct exact values registered in this index.
   *
   * @returns {IterableIterator<*>} The indexed values.
   */
  getKeys() {
    const self = this;
    return self._exact.keys();
  }

  /**
   * Resolve the row indices matching a filter value.
   *
   * @param {*} filterVal - The value to look up.
   * @param {boolean} allowMetaGroup - Whether a value ending in " - All"
   *     should be resolved via the meta-group prefix index rather than
   *     treated as an exact (and likely non-matching) value.
   * @returns {Set<number>} Matching row indices, possibly empty.
   */
  getIndicies(filterVal, allowMetaGroup) {
    const self = this;

    const isMetaGroup = allowMetaGroup && typeof filterVal === "string" &&
      filterVal.endsWith(" - All");

    if (isMetaGroup) {
      const prefix = filterVal.replaceAll(" - All", " - ");
      return self._metaGroupPrefix.get(prefix) || new Set();
    } else {
      return self._exact.get(filterVal) || new Set();
    }
  }

  /**
   * Add a row index under a key within a map of sets, creating the set if
   * this is the first row seen for that key.
   *
   * @private
   * @param {Map<*, Set<number>>} map - The map to add to.
   * @param {*} key - The key to add under.
   * @param {number} rowIndex - The row index to add.
   */
  _addToMap(map, key, rowIndex) {
    if (!map.has(key)) {
      map.set(key, new Set());
    }
    map.get(key).add(rowIndex);
  }

  /**
   * Register a row under every meta-group prefix implied by a string value.
   *
   * Splits on " - " and registers the row under each prefix formed by
   * rejoining leading segments (e.g. "A - B - C" registers under "A - " and
   * "A - B - "), mirroring the startsWith check in
   * ReportDataWrapper.stepWithSubtype. No-ops for non-string values (e.g.
   * year) or strings with no " - " separator.
   *
   * @private
   * @param {*} name - The value to derive meta-group prefixes from.
   * @param {number} rowIndex - The row index to register.
   */
  _addMetaGroupPrefixes(name, rowIndex) {
    const self = this;

    if (typeof name !== "string") {
      return;
    }

    const parts = name.split(" - ");
    let prefix = "";
    for (let i = 0; i < parts.length - 1; i++) {
      prefix += parts[i] + " - ";
      self._addToMap(self._metaGroupPrefix, prefix, rowIndex);
    }
  }
}

/**
 * Bundle of per-dimension indexes (scenario, year, application, substance)
 * over a single, fixed rows array.
 */
class IndexAccumulatorSet {
  /**
   * Create a new, empty index accumulator set.
   */
  constructor() {
    const self = this;

    /**
     * @private
     * @type {Array<EngineResult>|null}
     */
    self._rows = null;

    /**
     * @private
     * @type {IndexAccumulator}
     */
    self._byScenario = new IndexAccumulator();

    /**
     * @private
     * @type {IndexAccumulator}
     */
    self._byYear = new IndexAccumulator();

    /**
     * @private
     * @type {IndexAccumulator}
     */
    self._byApplication = new IndexAccumulator();

    /**
     * @private
     * @type {IndexAccumulator}
     */
    self._bySubstance = new IndexAccumulator();
  }

  /**
   * Index every row in an array.
   *
   * @param {Array<EngineResult>} rows - The rows to index.
   */
  indexRows(rows) {
    const self = this;
    self._rows = rows;
    rows.forEach((row, index) => self._indexRow(row, index));
  }

  /**
   * Get the rows array this instance was built from.
   *
   * @returns {Array<EngineResult>} The indexed rows.
   */
  getRows() {
    const self = this;
    return self._rows;
  }

  /**
   * Get the distinct scenario names registered in this index.
   *
   * @returns {IterableIterator<string>} The indexed scenario names.
   */
  getScenarioKeys() {
    const self = this;
    return self._byScenario.getKeys();
  }

  /**
   * Resolve row indices matching a scenario filter value.
   *
   * @param {string} scenario - The scenario name to look up.
   * @returns {Set<number>} Matching row indices.
   */
  getIndiciesByScenario(scenario) {
    const self = this;
    return self._byScenario.getIndicies(scenario, false);
  }

  /**
   * Resolve row indices matching a year filter value.
   *
   * @param {number} year - The year to look up.
   * @returns {Set<number>} Matching row indices.
   */
  getIndiciesByYear(year) {
    const self = this;
    return self._byYear.getIndicies(year, false);
  }

  /**
   * Resolve row indices matching an application filter value, including
   * "X - All" meta-group values.
   *
   * @param {string} application - The application (or meta-group) to look up.
   * @returns {Set<number>} Matching row indices.
   */
  getIndiciesByApplication(application) {
    const self = this;
    return self._byApplication.getIndicies(application, true);
  }

  /**
   * Resolve row indices matching a substance filter value, including
   * "X - All" meta-group values.
   *
   * @param {string} substance - The substance (or meta-group) to look up.
   * @returns {Set<number>} Matching row indices.
   */
  getIndiciesBySubstance(substance) {
    const self = this;
    return self._bySubstance.getIndicies(substance, true);
  }

  /**
   * Register a single row across all four dimension indexes.
   *
   * @private
   * @param {EngineResult} row - The row to index.
   * @param {number} index - The row's index in the rows array.
   */
  _indexRow(row, index) {
    const self = this;
    self._byScenario.addToIndex(row.getScenarioName(), index);
    self._byYear.addToIndex(row.getYear(), index);
    self._byApplication.addToIndex(row.getApplication(), index);
    self._bySubstance.addToIndex(row.getSubstance(), index);
  }
}

/**
 * Resolves a set of candidate row-index Sets down to matching rows.
 */
class RowSetResolver {
  /**
   * Create a new resolver.
   *
   * @param {Array<EngineResult>} allRows - The full rows array to resolve
   *     against and to return unchanged when no candidate sets are added.
   */
  constructor(allRows) {
    const self = this;
    self._allRows = allRows;
    self._candidateSets = [];
  }

  /**
   * Add a candidate row-index set that a matching row must belong to.
   *
   * @param {Set<number>} candidateSet - Row indices matching one filter
   *     dimension.
   */
  addCandidateSet(candidateSet) {
    const self = this;
    self._candidateSets.push(candidateSet);
  }

  /**
   * Resolve the rows matching every added candidate set.
   *
   * Returns all rows unchanged if no candidate sets were added (matching
   * ReportDataWrapper's behavior of returning the raw data as-is when every
   * filter field is null). Otherwise, intersects starting from the smallest
   * candidate set so cost is bounded by the most selective filter rather
   * than the full dataset. Each per-value set is populated in row order, so
   * iterating the smallest set preserves the original array's relative
   * ordering in the result.
   *
   * @returns {Array<EngineResult>} Matching rows, in original order.
   */
  resolve() {
    const self = this;

    if (self._candidateSets.length === 0) {
      return self._allRows;
    }

    const sorted = [...self._candidateSets].sort((a, b) => a.size - b.size);
    const smallest = sorted[0];
    const rest = sorted.slice(1);

    const matchingIndices = [];
    smallest.forEach((index) => {
      if (rest.every((set) => set.has(index))) {
        matchingIndices.push(index);
      }
    });

    return matchingIndices.map((index) => self._allRows[index]);
  }
}

/**
 * Drop-in replacement for ReportDataWrapper that indexes the raw results by
 * scenario, year, application, and substance instead of re-scanning the full
 * results array on every query.
 *
 * ReportDataWrapper._applyFilterSet performs up to four sequential
 * Array.filter passes over the entire raw results array on every call, with
 * no caching, even for a repeated identical filter. For large simulations
 * (many years/applications/substances/trials), the UI re-issues many such
 * queries per interaction (once per year for the chart, once per identifier
 * for each dimension card), making that cost visible as UI lag on every
 * click. This class builds indexes once (lazily, per import/export
 * attribution mode) and memoizes filtered/aggregated results, while
 * inheriting all other behavior (metric strategies, unit conversion, error
 * recovery) unchanged from ReportDataWrapper.
 */
class IndexedSimulationResult extends ReportDataWrapper {
  /**
   * Create a new indexed simulation result.
   *
   * @param {Array<EngineResult>} innerData - The raw report data to wrap.
   */
  constructor(innerData) {
    super(innerData);
    const self = this;

    /**
     * Lazily-built indexes, keyed by attribution mode ("importer"/"exporter").
     *
     * @private
     * @type {Map<string, IndexAccumulatorSet>}
     */
    self._indexes = new Map();

    /**
     * Memoized filtered row arrays, keyed by a serialized (year, scenario,
     * application, substance, attributeImporter) tuple.
     *
     * @private
     * @type {Map<string, Array<EngineResult>>}
     */
    self._rowFilterCache = new Map();

    /**
     * Memoized aggregated results, keyed by the same tuple as above.
     *
     * @private
     * @type {Map<string, AggregatedResult|null>}
     */
    self._aggregateCache = new Map();
  }

  /**
   * Get the set of distinct scenario names.
   *
   * Overridden because ReportDataWrapper's un-filtered branch maps over the
   * entire raw array on every call instead of going through
   * _applyFilterSet; here it is served directly from the scenario index.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {Set<string>} Set of scenario names.
   */
  getScenarios(filterSet) {
    const self = this;

    if (filterSet.getScenario() !== null) {
      return new Set([filterSet.getScenario()]);
    } else {
      const indexes = self._getOrBuildIndexes(filterSet);
      return new Set(indexes.getScenarioKeys());
    }
  }

  /**
   * Apply filter set to get matching results.
   *
   * @private
   * @param {FilterSet} filterSet - The filter criteria.
   * @returns {Array<EngineResult>} Array of matching results.
   */
  _applyFilterSet(filterSet) {
    const self = this;

    const cacheKey = self._makeCacheKey(filterSet);
    if (self._rowFilterCache.has(cacheKey)) {
      return self._rowFilterCache.get(cacheKey);
    } else {
      const indexes = self._getOrBuildIndexes(filterSet);
      const result = self._resolveFilteredRows(indexes, filterSet);
      self._rowFilterCache.set(cacheKey, result);
      return result;
    }
  }

  /**
   * Get aggregated result after applying filters.
   *
   * Memoized on top of the parent implementation, which itself calls back
   * into this instance's (now indexed) _applyFilterSet.
   *
   * @private
   * @param {FilterSet} filterSet - The filter criteria.
   * @returns {AggregatedResult|null} Aggregated result or null if no matches.
   */
  _getAggregatedAfterFilter(filterSet) {
    const self = this;

    const cacheKey = self._makeCacheKey(filterSet);
    if (self._aggregateCache.has(cacheKey)) {
      return self._aggregateCache.get(cacheKey);
    } else {
      const result = super._getAggregatedAfterFilter(filterSet);
      self._aggregateCache.set(cacheKey, result);
      return result;
    }
  }

  /**
   * Build a stable cache key from only the fields that affect row filtering.
   *
   * Metric/dimension/baseline/customDefinitions are deliberately excluded
   * since they do not affect which rows match.
   *
   * @private
   * @param {FilterSet} filterSet - The filter criteria.
   * @returns {string} A stable cache key.
   */
  _makeCacheKey(filterSet) {
    return [
      filterSet.getYear(),
      filterSet.getScenario(),
      filterSet.getApplication(),
      filterSet.getSubstance(),
      filterSet.getAttributeImporter(),
    ].map((value) => String(value)).join("|");
  }

  /**
   * Get (building if needed) the indexes for the attribution mode implied by
   * a filter set.
   *
   * @private
   * @param {FilterSet} filterSet - Used only to read the attribution mode and,
   *     on first build, to fetch the raw data for that mode.
   * @returns {IndexAccumulatorSet} The index bundle for this attribution mode.
   */
  _getOrBuildIndexes(filterSet) {
    const self = this;

    const attributionKey = filterSet.getAttributeImporter() ? "importer" : "exporter";
    if (self._indexes.has(attributionKey)) {
      return self._indexes.get(attributionKey);
    } else {
      return self._buildIndexes(attributionKey, filterSet);
    }
  }

  /**
   * Build and cache the indexes for an attribution mode.
   *
   * @private
   * @param {string} attributionKey - "importer" or "exporter".
   * @param {FilterSet} filterSet - Used only to fetch the raw data for this
   *     attribution mode.
   * @returns {IndexAccumulatorSet} The newly built index bundle.
   */
  _buildIndexes(attributionKey, filterSet) {
    const self = this;

    const rows = super.getRawData(filterSet);
    const indexes = new IndexAccumulatorSet();
    indexes.indexRows(rows);

    self._indexes.set(attributionKey, indexes);
    return indexes;
  }

  /**
   * Resolve the filtered rows for a filter set using the built indexes.
   *
   * @private
   * @param {IndexAccumulatorSet} indexes - The index bundle for this filter
   *     set's attribution mode.
   * @param {FilterSet} filterSet - The filter criteria.
   * @returns {Array<EngineResult>} Matching rows, in original order.
   */
  _resolveFilteredRows(indexes, filterSet) {
    const resolver = new RowSetResolver(indexes.getRows());

    const scenario = filterSet.getScenario();
    if (scenario !== null) {
      resolver.addCandidateSet(indexes.getIndiciesByScenario(scenario));
    }

    const year = filterSet.getYear();
    if (year !== null) {
      resolver.addCandidateSet(indexes.getIndiciesByYear(year));
    }

    const app = filterSet.getApplication();
    if (app !== null) {
      resolver.addCandidateSet(indexes.getIndiciesByApplication(app));
    }

    const sub = filterSet.getSubstance();
    if (sub !== null) {
      resolver.addCandidateSet(indexes.getIndiciesBySubstance(sub));
    }

    return resolver.resolve();
  }
}

export {IndexedSimulationResult, IndexAccumulator, IndexAccumulatorSet, RowSetResolver};
