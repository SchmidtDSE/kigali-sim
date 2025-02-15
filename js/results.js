/**
 * Presenters and logic to visualize engine result.
 *
 * @license BSD, see LICENSE.md.
 */
import {FilterSet} from "report_data";

/**
 * Array of colors used for visualizations
 * @type {string[]}
 */
const COLORS = [
  "#a6cee3",
  "#1f78b4",
  "#b2df8a",
  "#33a02c",
  "#fb9a99",
  "#e31a1c",
  "#505050",
  "#A0A0A0",
];

/**
 * Flag to allow an "All" option in filters to make it easy to return to a
 * default state.
 *
 * @type {boolean}
 */
const ALLOW_REDUNDANT_ALL = true;

/**
 * Flag to indicate if large score displays are active.
 *
 * @type {boolean}
 */
const ALLOW_SCORE_DISPLAY = false;

/**
 * Get a color from the predefined color palette.
 *
 * @param {number} i - Index into color array.
 * @returns {string} Color hex code.
 */
function getColor(i) {
  if (i >= COLORS.length) {
    return "#333";
  } else {
    return COLORS[i];
  }
}

/**
 * Main presenter class for displaying simulation results.
 */
class ResultsPresenter {
  /**
   * Create a new ResultsPresenter.
   *
   * @param {HTMLElement} root - Root DOM element for results display
   */
  constructor(root) {
    const self = this;
    self._root = root;
    self._results = null;
    self._filterSet = new FilterSet(
      null,
      null,
      null,
      null,
      "emissions:all:tCO2e / yr",
      "simulations",
    );

    const scorecardContainer = self._root.querySelector("#scorecards");
    const dimensionsContainer = self._root.querySelector("#dimensions");
    const centerChartContainer = self._root.querySelector("#center-chart");
    const centerChartHolderContainer = self._root.querySelector("#center-chart-holder");

    const onUpdateFilterSet = (x) => self._onUpdateFilterSet(x);
    self._scorecardPresenter = new ScorecardPresenter(scorecardContainer, onUpdateFilterSet);
    self._dimensionPresenter = new DimensionCardPresenter(dimensionsContainer, onUpdateFilterSet);
    self._centerChartPresenter = new CenterChartPresenter(centerChartContainer);
    self._titlePreseter = new SelectorTitlePresenter(centerChartHolderContainer, onUpdateFilterSet);
    self._exportPresenter = new ExportPresenter(self._root);

    self.hide();
  }

  /**
   * Hide the results display.
   */
  hide() {
    const self = this;
    self._root.style.display = "none";
  }

  /**
   * Show simulation results.
   *
   * @param {Object} results - Results data to display.
   */
  showResults(results) {
    const self = this;
    self._root.style.display = "block";
    self._results = results;
    self._updateInternally();
  }

  /**
   * Handle filter set updates.
   *
   * @param {FilterSet} newFilterSet - Updated filter settings.
   * @private
   */
  _onUpdateFilterSet(newFilterSet) {
    const self = this;
    self._filterSet = newFilterSet;
    self._updateInternally();
  }

  /**
   * Update all sub-presenters with current data.
   *
   * @private
   */
  _updateInternally() {
    const self = this;

    const years = self._results.getYears(self._filterSet.getWithYear(null));
    self._filterSet = self._filterSet.getWithYear(Math.max(...years));

    self._scorecardPresenter.showResults(self._results, self._filterSet);
    self._dimensionPresenter.showResults(self._results, self._filterSet);
    self._centerChartPresenter.showResults(self._results, self._filterSet);
    self._titlePreseter.showResults(self._results, self._filterSet);
    self._exportPresenter.showResults(self._results);
  }
}

/**
 * Presenter for configuring and executing results exports.
 */
class ExportPresenter {
  /**
   * Create a new ExportPresenter.
   *
   * @param {HTMLElement} root - Root DOM element.
   */
  constructor(root) {
    const self = this;
    self._root = root;
  }

  /**
   * Update export data with new results.
   *
   * @param {Object} results - Results data to export.
   */
  showResults(results) {
    const self = this;
    const rawData = results.getRawData();
    const nested = rawData.map((trial) => {
      const scenarioName = trial.getName();
      const results = trial.getTrialResults();
      return results
        .flat()
        .flat()
        .map((result) => {
          const application = result.getApplication();
          const substance = result.getSubstance();
          const year = result.getYear();
          const manufactureValue = result.getManufacture();
          const importValue = result.getImport();
          const rechargeEmissionsValue = result.getRechargeEmissions();
          const eolEmissionsValue = result.getEolEmissions();
          const populationValue = result.getPopulation();
          return {
            scenario: scenarioName,
            application: application,
            substance: substance,
            year: year,
            manufactureValue: manufactureValue.getValue(),
            manufactureUnits: manufactureValue.getUnits(),
            importValue: importValue.getValue(),
            importUnits: importValue.getUnits(),
            rechargeEmissionsValue: rechargeEmissionsValue.getValue(),
            rechargeEmissionsValue: rechargeEmissionsValue.getUnits(),
            eolEmissionsValue: eolEmissionsValue.getValue(),
            eolEmissionsUnits: eolEmissionsValue.getUnits(),
            equipmentPopulation: populationValue.getValue(),
            equipmentUnits: populationValue.getUnits(),
          };
        });
    });
    const flat = nested.flat();
    const contentRows = flat.map((record) => {
      const vals = [
        record["scenario"],
        record["application"],
        record["substance"],
        record["year"],
        record["manufactureValue"],
        record["manufactureUnits"],
        record["importValue"],
        record["importUnits"],
        record["rechargeEmissionsValue"],
        record["rechargeEmissionsUnits"],
        record["eolEmissionsValue"],
        record["eolEmissionsUnits"],
        record["equipmentPopulation"],
        record["equipmentUnits"],
      ];
      const valsStrs = vals.map((x) => x + "");
      const valsStr = valsStrs.join(",");
      return valsStr;
    });
    const contentStr = contentRows.join("\n");

    const headerElements = [
      "scenario",
      "application",
      "substance",
      "year",
      "manufactureValue",
      "manufactureUnits",
      "importValue",
      "importUnits",
      "rechargeEmissionsValue",
      "rechargeEmisionsUnits",
      "eolEmissionsValue",
      "eolEmisionsUnits",
      "equipmentPopulation",
      "equipmentUnits",
    ];
    const headerStr = headerElements.join(",");

    const csvStr = headerStr + "\n" + contentStr;
    const encodedValue = encodeURI("data:text/csv;charset=utf-8," + csvStr);
    const exportLink = document.getElementById("export-button");
    exportLink.href = encodedValue;
  }
}

/**
 * Presenter for scorecard metrics display.
 *
 * Presenter for scorecard metrics display that, in addition to optionally
 * showing a high level metric, also allows for changing filter values.
 */
class ScorecardPresenter {
  /**
   * Create a new ScorecardPresenter.
   *
   * @param {HTMLElement} root - Root DOM element.
   * @param {Function} onUpdateFilterSet - Callback for filter updates.
   */
  constructor(root, onUpdateFilterSet) {
    const self = this;
    self._root = root;
    self._filterSet = null;
    self._onUpdateFilterSet = onUpdateFilterSet;
    self._registerEventListeners();
  }

  /**
   * Update scorecards with new results data.
   *
   * @param {Object} results - Results data to display.
   * @param {FilterSet} filterSet - Current filter settings.
   */
  showResults(results, filterSet) {
    const self = this;
    self._filterSet = filterSet;
    const currentYear = self._filterSet.getYear();

    const emissionsScorecard = self._root.querySelector("#emissions-scorecard");
    const salesScorecard = self._root.querySelector("#sales-scorecard");
    const equipmentScorecard = self._root.querySelector("#equipment-scorecard");

    const emissionsValue = results.getTotalEmissions(filterSet);
    const salesValue = results.getSales(filterSet);
    const equipmentValue = results.getPopulation(filterSet);

    const roundToTenths = (x) => Math.round(x * 10) / 10;
    const emissionRounded = roundToTenths(emissionsValue.getValue() / 1000000);
    const salesMt = roundToTenths(salesValue.getValue() / 1000000) + " k";
    const millionEqipment = roundToTenths(equipmentValue.getValue() / 1000000) + " M";

    const metricSelected = filterSet.getMetric();
    const emissionsSelected = metricSelected === "emissions";
    const salesSelected = metricSelected === "sales";
    const equipmentSelected = metricSelected === "population";

    const scenarios = results.getScenarios(self._filterSet.getWithScenario(null));
    const showVal = ALLOW_SCORE_DISPLAY && self._filterSet.hasSingleScenario(scenarios);
    const hideVal = !showVal;

    self._updateCard(emissionsScorecard, emissionRounded, currentYear, emissionsSelected, hideVal);
    self._updateCard(salesScorecard, salesMt, currentYear, salesSelected, hideVal);
    self._updateCard(equipmentScorecard, millionEqipment, currentYear, equipmentSelected, hideVal);
  }

  /**
   * Update an individual scorecard.
   *
   * @param {HTMLElement} scorecard - Scorecard DOM element.
   * @param {string|number} value - Value to display.
   * @param {number} currentYear - Current year.
   * @param {boolean} selected - Flag indicating if the card is selected.
   * @param {boolean} hideVal - Flag indicating if the value display should be
   *     hidden. True if a value should be displayed and false otherwise which
   *     may be appropriate if a single summary value cannot be generated.
   * @private
   */
  _updateCard(scorecard, value, currentYear, selected, hideVal) {
    const self = this;
    self._setText(scorecard.querySelector(".value"), value);

    if (hideVal) {
      scorecard.querySelector(".value").style.display = "none";
    } else {
      scorecard.querySelector(".value").style.display = "block";
    }

    if (selected) {
      scorecard.classList.remove("inactive");
    } else {
      scorecard.classList.add("inactive");
    }

    d3.select(scorecard).select(".metric-radio").property("checked", selected);
  }

  /**
   * Set text content of an element.
   *
   * @param {HTMLElement} selection - Element to update.
   * @param {string} value - New text value.
   * @private
   */
  _setText(selection, value) {
    const self = this;
    selection.innerHTML = "";
    const newTextNode = document.createTextNode(value);
    selection.appendChild(newTextNode);
  }

  /**
   * Register event listeners for scorecard interactions.
   *
   * @private
   */
  _registerEventListeners() {
    const self = this;

    const emissionsScorecard = self._root.querySelector("#emissions-scorecard");
    const salesScorecard = self._root.querySelector("#sales-scorecard");
    const equipmentScorecard = self._root.querySelector("#equipment-scorecard");

    const registerListener = (scorecard, family) => {
      const subMetricDropdown = scorecard.querySelector(".submetric-input");
      const unitsDropdown = scorecard.querySelector(".units-input");

      const callback = () => {
        const subMetric = subMetricDropdown.value;
        const units = unitsDropdown.value;
        const fullName = family + ":" + subMetric + ":" + units;
        const newFilterSet = self._filterSet.getWithMetric(fullName);
        self._onUpdateFilterSet(newFilterSet);
      };

      const radio = scorecard.querySelector(".metric-radio");
      radio.addEventListener("click", callback);

      subMetricDropdown.addEventListener("change", callback);
      unitsDropdown.addEventListener("change", callback);
    };

    registerListener(emissionsScorecard, "emissions");
    registerListener(salesScorecard, "sales");
    registerListener(equipmentScorecard, "population");
  }
}

/**
 * Presenter for dimension cards display.
 *
 * Presenter which shows the dimensions selectors with embedded bar charts that
 * allow for changing of filters.
 */
class DimensionCardPresenter {
  /**
   * Create a new DimensionCardPresenter.
   *
   * @param {HTMLElement} root - Root DOM element.
   * @param {Function} onUpdateFilterSet - Callback for filter updates.
   */
  constructor(root, onUpdateFilterSet) {
    const self = this;
    self._root = root;
    self._onUpdateFilterSet = onUpdateFilterSet;
    self._filterSet = null;
    self._registerEventListeners();
  }

  /**
   * Update dimension cards with new results
   * @param {Object} results - Results data to display
   * @param {FilterSet} filterSet - Current filter settings
   */
  showResults(results, filterSet) {
    const self = this;
    self._filterSet = filterSet;

    const metricSelected = self._filterSet.getMetric();
    const metricUnits = self._filterSet.getUnits();

    const currentYear = self._filterSet.getYear();
    const scenarios = results.getScenarios(self._filterSet.getWithScenario(null));

    const allTickUnits = Array.of(...self._root.querySelectorAll(".units-tick"));
    allTickUnits.forEach((x) => (x.innerHTML = metricUnits));

    const allTickYears = Array.of(...self._root.querySelectorAll(".years-tick"));
    if (self._filterSet.hasSingleScenario(scenarios)) {
      allTickYears.forEach((x) => (x.innerHTML = "in year " + currentYear));
    } else {
      allTickYears.forEach((x) => (x.innerHTML = ""));
    }

    const simulationsCard = self._root.querySelector("#simulations-dimension");
    const applicationsCard = self._root.querySelector("#applications-dimension");
    const substancesCard = self._root.querySelector("#substances-dimension");

    const dimensionSelected = self._filterSet.getDimension();
    const simulationsSelected = dimensionSelected === "simulations";
    const applicationsSelected = dimensionSelected === "applications";
    const substancesSelected = dimensionSelected === "substances";

    const interpret = (x) => (x === null ? null : x.getValue());

    self._updateCard(
      "sim",
      simulationsCard,
      results.getScenarios(),
      simulationsSelected,
      self._filterSet.getScenario(),
      (x) => self._filterSet.getWithScenario(x),
      true,
      (value) => interpret(results.getMetric(self._filterSet.getWithScenario(value))),
      metricUnits,
      scenarios,
    );

    self._updateCard(
      "app",
      applicationsCard,
      results.getApplications(self._filterSet.getWithApplication(null)),
      applicationsSelected,
      self._filterSet.getApplication(),
      (x) => self._filterSet.getWithApplication(x),
      true,
      (value) => interpret(results.getMetric(self._filterSet.getWithApplication(value))),
      metricUnits,
      scenarios,
    );

    self._updateCard(
      "sub",
      substancesCard,
      results.getSubstances(self._filterSet.getWithSubstance(null)),
      substancesSelected,
      self._filterSet.getSubstance(),
      (x) => self._filterSet.getWithSubstance(x),
      true,
      (value) => interpret(results.getMetric(self._filterSet.getWithSubstance(value))),
      metricUnits,
      scenarios,
    );
  }

  /**
   * Update an individual dimension card.
   *
   * @param {string} label - Card identifier.
   * @param {HTMLElement} card - Card DOM element.
   * @param {Set<string>} identifiers - Set of dimension values.
   * @param {boolean} selected - Whether card is selected.
   * @param {string} subSelection - Currently selected value.
   * @param {Function} subFilterSetBuilder - Filter builder function.
   * @param {boolean} addAll - Whether to add "All" option. True if should be
   *     added and false otherwise.
   * @param {Function} valueGetter - Function to get display value.
   * @param {string} suffix - Value suffix like for units.
   * @param {Set<string>} scenarios - Available scenarios.
   * @private
   */
  _updateCard(
    label,
    card,
    identifiers,
    selected,
    subSelection,
    subFilterSetBuilder,
    addAll,
    valueGetter,
    suffix,
    scenarios,
  ) {
    const self = this;

    if (selected) {
      card.classList.remove("inactive");
    } else {
      card.classList.add("inactive");
    }

    d3.select(card).select(".dimension-radio").property("checked", selected);

    const identifiersArray = Array.of(...identifiers);
    identifiersArray.sort();

    const values = identifiersArray.map(valueGetter);
    const maxValue = Math.max(...values);
    d3.select(card.querySelector(".right-tick")).text(Math.round(maxValue));

    const hasSingleScenario = self._filterSet.hasSingleScenario(scenarios);
    const isOnlyValue = identifiersArray.length == 1;

    const allNeeded = ALLOW_REDUNDANT_ALL ? addAll : addAll && !isOnlyValue;
    if (allNeeded) {
      identifiersArray.unshift("All");
    }

    const listSelection = d3.select(card).select(".list");
    listSelection.html("");

    const itemDivs = listSelection
      .selectAll(".item")
      .data(identifiersArray)
      .enter()
      .append("div")
      .classed("item", true);

    const itemLabels = itemDivs.append("label");

    itemLabels
      .append("input")
      .attr("type", "radio")
      .classed(label + "-radio", true)
      .attr("name", label + "-viz")
      .style("height", "13px")
      .style("width", "13px")
      .property("checked", (x) => {
        const valuesMatch = x === subSelection;
        const isAllAndSelected = subSelection === null && x === "All";
        return valuesMatch || isAllAndSelected || isOnlyValue;
      });

    itemLabels.append("span").text((x) => x);

    itemLabels.on("click", (event, x) => {
      const newFilterSet = subFilterSetBuilder(x === "All" ? null : x);
      self._onUpdateFilterSet(newFilterSet);
    });

    if (hasSingleScenario || label === "sim") {
      const offset = allNeeded ? 1 : 0;
      const lines = itemDivs
        .append("div")
        .classed("list-line", true)
        .style("width", "100%")
        .style("height", (x, i) => (x === "All" ? "0px" : "1px"))
        .style("background-color", (x, i) => (selected ? getColor(i - offset) : "#C0C0C0"));

      lines
        .append("div")
        .classed("list-bar", true)
        .style("height", (x, i) => (x === "All" ? "0px" : "5px"))
        .style("background-color", (x, i) => (selected ? getColor(i - offset) : "#C0C0C0"))
        .style("width", (x) => {
          if (x === "All") {
            return "0%";
          } else {
            const value = valueGetter(x);
            const percent = value / maxValue;
            return Math.round(percent * 100) + "%";
          }
        });

      d3.select(card).select(".axis").style("display", "grid");
    } else {
      d3.select(card).select(".axis").style("display", "none");
    }
  }

  /**
   * Register event listeners for dimension card interactions.
   *
   * @private
   */
  _registerEventListeners() {
    const self = this;

    const simulationsCard = self._root.querySelector("#simulations-dimension");
    const applicationsCard = self._root.querySelector("#applications-dimension");
    const substancesCard = self._root.querySelector("#substances-dimension");

    const registerListener = (scorecard, value) => {
      const radio = scorecard.querySelector(".dimension-radio");
      radio.addEventListener("click", () => {
        const newFilterSet = self._filterSet.getWithDimension(value);
        self._onUpdateFilterSet(newFilterSet);
      });
    };

    registerListener(simulationsCard, "simulations");
    registerListener(applicationsCard, "applications");
    registerListener(substancesCard, "substances");
  }
}

/**
 * Presenter for the central chart visualization.
 *
 * Presenter for the central chart visualization which is currently backed by
 * Chartjs.
 */
class CenterChartPresenter {
  /**
   * Create a new CenterChartPresenter.
   *
   * @param {HTMLElement} root - Root DOM element
   */
  constructor(root) {
    const self = this;
    self._root = root;
    self._chart = null;
  }

  /**
   * Update chart with new results.
   *
   * @param {Object} results - Results data to display.
   * @param {FilterSet} filterSet - Current filter settings.
   */
  showResults(results, filterSet) {
    const self = this;

    if (self._chart !== null) {
      self._chart.destroy();
    }

    const years = Array.of(...results.getYears(filterSet.getWithYear(null)));
    years.sort((a, b) => a - b);

    const dimensionValues = Array.of(...results.getDimensionValues(filterSet));
    dimensionValues.sort();

    const getForDimValue = (dimValue) => {
      const valsWithUnits = years.map((year) => {
        const withYear = filterSet.getWithYear(year);
        const subFilterSet = withYear.getWithDimensionValue(dimValue);
        return results.getMetric(subFilterSet);
      });
      const valsWithUnitsValid = valsWithUnits.filter((x) => x !== null);
      const vals = valsWithUnitsValid.map((x) => x.getValue());
      return {name: dimValue, vals: vals};
    };
    const dimensionSeries = dimensionValues.map(getForDimValue);

    const unconstrainedDimValues = Array.of(
      ...results.getDimensionValues(filterSet.getWithDimensionValue(null)),
    );
    unconstrainedDimValues.sort();

    const chartJsDatasets = dimensionSeries.map((x) => {
      const index = unconstrainedDimValues.indexOf(x["name"]);
      const color = getColor(index);
      return {
        label: x["name"],
        data: x["vals"],
        fill: false,
        borderColor: color,
        backgroundColor: color,
      };
    });

    const chartJsData = {
      labels: years,
      datasets: chartJsDatasets,
    };

    const metricSelected = filterSet.getMetric();
    const metricUnits = filterSet.getUnits();

    const chartJsConfig = {
      type: "line",
      data: chartJsData,
      options: {
        scales: {
          y: {
            min: 0,
            title: {text: metricUnits, display: true},
          },
          x: {
            title: {text: "Year", display: true},
          },
        },
        plugins: {
          tooltip: {
            callbacks: {
              title: (x) => "Year " + x[0]["label"],
            },
          },
          legend: {
            display: false,
          },
        },
      },
    };

    self._chart = new Chart(self._root, chartJsConfig);
  }
}

/**
 * Presenter for selector title display.
 *
 * Presenter for selector title display using a fill in the blank-like
 * approach.
 */
class SelectorTitlePresenter {
  /**
   * Create a new SelectorTitlePresenter.
   *
   * @param {HTMLElement} root - Root DOM element.
   * @param {Function} changeCallback - Callback for selection changes.
   */
  constructor(root, changeCallback) {
    const self = this;
    self._selection = root;
    self._changeCallback = changeCallback;
    self._filterSet = null;
    self._setupEventListeners();
  }

  /**
   * Update selector title with new results.
   *
   * @param {Object} results - Results data to display.
   * @param {FilterSet} filterSet - Current filter settings.
   */
  showResults(results, filterSet) {
    const self = this;
    self._filterSet = filterSet;

    const metricDropdown = self._selection.querySelector(".metric-select");
    const metricSelected = self._filterSet.getMetric();
    self._updateSimpleDropdown(metricDropdown, metricSelected);

    const dimensionDropdown = self._selection.querySelector(".dimension-select");
    const dimensionSelected = self._filterSet.getDimension();
    self._updateSimpleDropdown(dimensionDropdown, dimensionSelected);

    const scenarioDropdown = self._selection.querySelector(".scenario-select");
    const scenarioSelected = self._filterSet.getScenario();
    const scenarios = results.getScenarios();
    self._updateDynamicDropdown(scenarioDropdown, scenarios, scenarioSelected, "All Simulations");

    const applicationDropdown = self._selection.querySelector(".application-select");
    const applicationSelected = self._filterSet.getApplication();
    const applications = results.getApplications(self._filterSet.getWithApplication(null));
    self._updateDynamicDropdown(
      applicationDropdown,
      applications,
      applicationSelected,
      "All Applications",
    );

    const substanceDropdown = self._selection.querySelector(".substance-select");
    const substanceSelected = self._filterSet.getSubstance();
    const substances = results.getSubstances(self._filterSet.getWithSubstance(null));
    self._updateDynamicDropdown(substanceDropdown, substances, substanceSelected, "All Substances");
  }

  /**
   * Update a simple dropdown with fixed options.
   *
   * @param {HTMLElement} selection - Dropdown element.
   * @param {string} value - Selected value.
   * @private
   */
  _updateSimpleDropdown(selection, value) {
    const self = this;
    selection.value = value;
  }

  /**
   * Update a dynamic dropdown with variable options.
   *
   * @param {HTMLElement} selection - Dropdown element.
   * @param {Set<string>} allValues - Available values.
   * @param {string} selectedValue - Currently selected value.
   * @param {string} allText - Text for "All" option.
   * @private
   */
  _updateDynamicDropdown(selection, allValues, selectedValue, allText) {
    const self = this;

    const allValuesArray = Array.of(...allValues);
    allValuesArray.unshift(allText);
    allValuesArray.sort();

    const d3Selection = d3.select(selection);
    d3Selection.html("");
    d3Selection
      .selectAll("option")
      .data(allValuesArray)
      .enter()
      .append("option")
      .attr("value", (x) => (allText === x ? "" : x))
      .text((x) => x)
      .property("selected", (x) => {
        const nativelySelected = x === selectedValue;
        const allSelected = x === allText && selectedValue === null;
        return nativelySelected || allSelected;
      });
  }

  /**
   * Set up event listeners for selector dropdowns.
   *
   * @private
   */
  _setupEventListeners() {
    const self = this;

    const addListener = (selection, newFilterSetGen) => {
      selection.addEventListener("change", () => {
        const value = selection.value === "" ? null : selection.value;
        const newFilterSet = newFilterSetGen(self._filterSet, value);
        self._changeCallback(newFilterSet);
      });
    };

    const metricDropdown = self._selection.querySelector(".metric-select");
    addListener(metricDropdown, (filterSet, val) => filterSet.getWithMetric(val));

    const dimensionDropdown = self._selection.querySelector(".dimension-select");
    addListener(dimensionDropdown, (filterSet, val) => filterSet.getWithDimension(val));

    const scenarioDropdown = self._selection.querySelector(".scenario-select");
    addListener(scenarioDropdown, (filterSet, val) => filterSet.getWithScenario(val));

    const applicationDropdown = self._selection.querySelector(".application-select");
    addListener(applicationDropdown, (filterSet, val) => filterSet.getWithApplication(val));

    const substanceDropdown = self._selection.querySelector(".substance-select");
    addListener(substanceDropdown, (filterSet, val) => filterSet.getWithSubstance(val));
  }
}

export {ResultsPresenter};
