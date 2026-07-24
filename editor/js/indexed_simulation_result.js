/**
 * Indexed, memoized wrapper over simulation results for fast repeated queries.
 *
 * @license BSD, see LICENSE.md.
 */

import {ReportDataWrapper} from "report_data";

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
     * @type {Map<string, Object>}
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
    }

    const indexes = self._getOrBuildIndexes(filterSet);
    return new Set(indexes.byScenario.keys());
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
    }

    const indexes = self._getOrBuildIndexes(filterSet);
    const result = self._resolveFilteredRows(indexes, filterSet);
    self._rowFilterCache.set(cacheKey, result);
    return result;
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
    }

    const result = super._getAggregatedAfterFilter(filterSet);
    self._aggregateCache.set(cacheKey, result);
    return result;
  }

  /**
   * Build a stable cache key from only the fields that affect row filtering.
   *
   * Metric/dimension/baseline/customDefinitions are deliberately excluded
   * since they do not affect which rows match.
   *
   * @private
   * @param {FilterSet} filterSet - The filter criteria.
   * @returns {string} A stable, collision-free cache key.
   */
  _makeCacheKey(filterSet) {
    return JSON.stringify([
      filterSet.getYear(),
      filterSet.getScenario(),
      filterSet.getApplication(),
      filterSet.getSubstance(),
      filterSet.getAttributeImporter(),
    ]);
  }

  /**
   * Get (building if needed) the indexes for the attribution mode implied by
   * a filter set.
   *
   * @private
   * @param {FilterSet} filterSet - Used only to read the attribution mode and,
   *     on first build, to fetch the raw data for that mode.
   * @returns {Object} The index bundle for this attribution mode.
   */
  _getOrBuildIndexes(filterSet) {
    const self = this;

    const attributionKey = filterSet.getAttributeImporter() ? "importer" : "exporter";
    if (self._indexes.has(attributionKey)) {
      return self._indexes.get(attributionKey);
    }

    const rows = super.getRawData(filterSet);

    const byScenario = new Map();
    const byYear = new Map();
    const byApplicationExact = new Map();
    const byApplicationMetaPrefix = new Map();
    const bySubstanceExact = new Map();
    const bySubstanceMetaPrefix = new Map();

    const addToIndex = (map, key, rowIndex) => {
      if (!map.has(key)) {
        map.set(key, new Set());
      }
      map.get(key).add(rowIndex);
    };

    // Register every " - " boundary as a possible meta-group prefix (e.g. "A - B - C"
    // registers under both "A - " and "A - B - "), mirroring the startsWith check in
    // ReportDataWrapper's stepWithSubtype so meta-group ("X - All") filters resolve to
    // an exact index lookup instead of a scan.
    const addMetaGroupPrefixes = (map, name, rowIndex) => {
      let searchFrom = 0;
      while (true) {
        const sepIndex = name.indexOf(" - ", searchFrom);
        if (sepIndex === -1) {
          break;
        }
        const prefix = name.substring(0, sepIndex + 3);
        addToIndex(map, prefix, rowIndex);
        searchFrom = sepIndex + 3;
      }
    };

    rows.forEach((row, index) => {
      addToIndex(byScenario, row.getScenarioName(), index);
      addToIndex(byYear, row.getYear(), index);

      const application = row.getApplication();
      addToIndex(byApplicationExact, application, index);
      addMetaGroupPrefixes(byApplicationMetaPrefix, application, index);

      const substance = row.getSubstance();
      addToIndex(bySubstanceExact, substance, index);
      addMetaGroupPrefixes(bySubstanceMetaPrefix, substance, index);
    });

    const built = {
      rows: rows,
      byScenario: byScenario,
      byYear: byYear,
      byApplicationExact: byApplicationExact,
      byApplicationMetaPrefix: byApplicationMetaPrefix,
      bySubstanceExact: bySubstanceExact,
      bySubstanceMetaPrefix: bySubstanceMetaPrefix,
    };
    self._indexes.set(attributionKey, built);
    return built;
  }

  /**
   * Resolve the row-index Set for an application or substance filter value,
   * handling the "X - All" meta-group case exactly as
   * ReportDataWrapper.stepWithSubtype does.
   *
   * @private
   * @param {string} filterVal - The application or substance filter value.
   * @param {Map<string, Set<number>>} exactMap - Index of exact-match values.
   * @param {Map<string, Set<number>>} metaPrefixMap - Index of meta-group
   *     prefixes.
   * @returns {Set<number>} Matching row indices, possibly empty.
   */
  _resolveSubtypeSet(filterVal, exactMap, metaPrefixMap) {
    const isMetaGroup = filterVal.endsWith(" - All");
    if (!isMetaGroup) {
      return exactMap.get(filterVal) || new Set();
    }

    const withAllReplace = filterVal.replaceAll(" - All", " - ");
    return metaPrefixMap.get(withAllReplace) || new Set();
  }

  /**
   * Resolve the filtered rows for a filter set using the built indexes.
   *
   * @private
   * @param {Object} indexes - The index bundle for this filter set's
   *     attribution mode.
   * @param {FilterSet} filterSet - The filter criteria.
   * @returns {Array<EngineResult>} Matching rows, in original order.
   */
  _resolveFilteredRows(indexes, filterSet) {
    const self = this;

    const candidateSets = [];

    const scenario = filterSet.getScenario();
    if (scenario !== null) {
      candidateSets.push(indexes.byScenario.get(scenario) || new Set());
    }

    const year = filterSet.getYear();
    if (year !== null) {
      candidateSets.push(indexes.byYear.get(year) || new Set());
    }

    const app = filterSet.getApplication();
    if (app !== null) {
      candidateSets.push(
        self._resolveSubtypeSet(app, indexes.byApplicationExact, indexes.byApplicationMetaPrefix),
      );
    }

    const sub = filterSet.getSubstance();
    if (sub !== null) {
      candidateSets.push(
        self._resolveSubtypeSet(sub, indexes.bySubstanceExact, indexes.bySubstanceMetaPrefix),
      );
    }

    if (candidateSets.length === 0) {
      return indexes.rows;
    }

    // Intersect starting from the smallest set so cost is bounded by the most
    // selective filter rather than the full dataset. Each per-value Set was
    // populated in row order, so iterating the smallest set preserves the
    // original array's relative ordering in the result.
    candidateSets.sort((a, b) => a.size - b.size);
    const smallest = candidateSets[0];
    const rest = candidateSets.slice(1);

    const matchingIndices = [];
    smallest.forEach((index) => {
      if (rest.every((set) => set.has(index))) {
        matchingIndices.push(index);
      }
    });

    return matchingIndices.map((index) => indexes.rows[index]);
  }
}

export {IndexedSimulationResult};
