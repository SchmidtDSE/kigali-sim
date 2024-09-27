import {FilterSet} from "report_data";

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

const ALLOW_REDUNDANT_ALL = true;


function getColor(i) {
  if (i >= COLORS.length) {
    return "#333";
  } else {
    return COLORS[i];
  }
}


class ScorecardPresenter {
  constructor(targetId, onUpdateFilterSet) {
    const self = this;
    self._root = document.getElementById(targetId);
    self._filterSet = null;
    self._onUpdateFilterSet = onUpdateFilterSet;
    self._registerEventListeners();
  }

  showResults(results, filterSet) {
    const self = this;
    self._filterSet = filterSet;
    const currentYear = self._filterSet.getYear();

    const emissionsScorecard = self._root.querySelector("#emissions-scorecard");
    const salesScorecard = self._root.querySelector("#sales-scorecard");
    const equipmentScorecard = self._root.querySelector("#equipment-scorecard");

    const emissionsValue = results.getEmissions(filterSet);
    const salesValue = results.getSales(filterSet);
    const equipmentValue = results.getPopulation(filterSet);

    const emissionRounded = Math.round(emissionsValue.getValue()) / 1000000;
    const salesMt = Math.round(salesValue.getValue() / 1000);
    const millionEqipment = Math.round(equipmentValue.getValue() / 1000000) + "M";

    const metricSelected = filterSet.getMetric();
    const emissionsSelected = metricSelected === "emissions";
    const salesSelected = metricSelected === "sales";
    const equipmentSelected = metricSelected === "population";

    const scenarios = results.getScenarios(self._filterSet.getWithScenario(null));
    const hideVal = !self._filterSet.hasSingleScenario(scenarios);

    self._updateCard(emissionsScorecard, emissionRounded, currentYear, emissionsSelected, hideVal);
    self._updateCard(salesScorecard, salesMt, currentYear, salesSelected, hideVal);
    self._updateCard(equipmentScorecard, millionEqipment, currentYear, equipmentSelected, hideVal);
  }

  _updateCard(scorecard, value, currentYear, selected, hideVal) {
    const self = this;
    self._setText(scorecard.querySelector(".value"), value);

    if (hideVal) {
      self._setText(scorecard.querySelector(".current-year"), "");
    } else {
      self._setText(scorecard.querySelector(".current-year"), "in year " + currentYear);
    }

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
  }

  _setText(selection, value) {
    const self = this;
    selection.innerHTML = "";
    const newTextNode = document.createTextNode(value);
    selection.appendChild(newTextNode);
  }

  _registerEventListeners() {
    const self = this;

    const emissionsScorecard = self._root.querySelector("#emissions-scorecard");
    const salesScorecard = self._root.querySelector("#sales-scorecard");
    const equipmentScorecard = self._root.querySelector("#equipment-scorecard");

    const registerListener = (scorecard, value) => {
      const radio = scorecard.querySelector(".metric-radio");
      radio.addEventListener("click", () => {
        const newFilterSet = self._filterSet.getWithMetric(value);
        self._onUpdateFilterSet(newFilterSet);
      });
    };

    registerListener(emissionsScorecard, "emissions");
    registerListener(salesScorecard, "sales");
    registerListener(equipmentScorecard, "population");
  }
}


class DimensionCardPresenter {
  constructor(targetId, onUpdateFilterSet) {
    const self = this;
    self._root = document.getElementById(targetId);
    self._onUpdateFilterSet = onUpdateFilterSet;
    self._filterSet = null;
    self._registerEventListeners();
  }

  showResults(results, filterSet) {
    const self = this;
    self._filterSet = filterSet;

    const metricSelected = self._filterSet.getMetric();
    const metricUnits = {
      "emissions": "MtCO2e / yr",
      "sales": "mt / yr",
      "population": "units",
    }[metricSelected];

    const currentYear = self._filterSet.getYear();
    const scenarios = results.getScenarios(self._filterSet.getWithScenario(null));

    const allTickUnits = Array.of(...self._root.querySelectorAll(".units-tick"));
    allTickUnits.forEach((x) => x.innerHTML = metricUnits);

    const allTickYears = Array.of(...self._root.querySelectorAll(".years-tick"));
    if (self._filterSet.hasSingleScenario(scenarios)) {
      allTickYears.forEach((x) => x.innerHTML = "in year " + currentYear);
    } else {
      allTickYears.forEach((x) => x.innerHTML = "");
    }

    const simulationsCard = self._root.querySelector("#simulations-dimension");
    const applicationsCard = self._root.querySelector("#applications-dimension");
    const substancesCard = self._root.querySelector("#substances-dimension");

    const dimensionSelected = self._filterSet.getDimension();
    const simulationsSelected = dimensionSelected === "simulations";
    const applicationsSelected = dimensionSelected === "applications";
    const substancesSelected = dimensionSelected === "substances";

    const conversionInfo = {
      "emissions": {"divider": 1000000, "suffix": "M"},
      "sales": {"divider": 1000, "suffix": ""},
      "population": {"divider": 1000000, "suffix": "M"},
    }[self._filterSet.getMetric()];
    const divider = conversionInfo["divider"];
    const suffix = conversionInfo["suffix"];
    const interpret = (x) => x.getValue() / divider;

    self._updateCard(
      "sim",
      simulationsCard,
      results.getScenarios(),
      simulationsSelected,
      self._filterSet.getScenario(),
      (x) => self._filterSet.getWithScenario(x),
      true,
      (value) => interpret(results.getMetric(self._filterSet.getWithScenario(value))),
      suffix,
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
      suffix,
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
      suffix,
      scenarios,
    );
  }

  _updateCard(label, card, identifiers, selected, subSelection, subFilterSetBuilder, addAll,
    valueGetter, suffix, scenarios, selectedYear) {
    const self = this;

    if (selected) {
      card.classList.remove("inactive");
    } else {
      card.classList.add("inactive");
    }

    const identifiersArray = Array.of(...identifiers);
    identifiersArray.sort();

    const values = identifiersArray.map(valueGetter);
    const maxValue = Math.max(...values);
    d3.select(card.querySelector(".right-tick")).text(Math.round(maxValue) + suffix);

    const hasSingleScenario = self._filterSet.hasSingleScenario(scenarios);
    const isOnlyValue = identifiersArray.length == 1;

    const allNeeded = ALLOW_REDUNDANT_ALL ? addAll : addAll && !isOnlyValue;
    if (allNeeded) {
      identifiersArray.unshift("All");
    }

    const listSelection = d3.select(card).select(".list");
    listSelection.html("");

    const itemDivs = listSelection.selectAll(".item")
      .data(identifiersArray)
      .enter()
      .append("div")
      .classed("item", true);

    const itemLabels = itemDivs.append("label");

    itemLabels.append("input")
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
      const lines = itemDivs.append("div")
        .classed("list-line", true)
        .style("width", "100%")
        .style("height", (x, i) => x === "All" ? "0px" : "1px")
        .style("background-color", (x, i) => selected ? getColor(i - offset) : "#C0C0C0");

      lines.append("div")
        .classed("list-bar", true)
        .style("height", (x, i) => x === "All" ? "0px" : "5px")
        .style("background-color", (x, i) => selected ? getColor(i - offset) : "#C0C0C0")
        .style("width", (x) => {
          if (x === "All") {
            return "0%";
          } else {
            const value = valueGetter(x);
            const percent = value / maxValue;
            return Math.round(percent * 100) + "%";
          }
        });
    }
  }

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


class CenterChartPresenter {
  constructor(targetId) {
    const self = this;
    self._targetId = targetId;
    self._chart = null;
  }

  showResults(results, filterSet) {
    const self = this;

    if (self._chart !== null) {
      self._chart.destroy();
    }

    const years = Array.of(...results.getYears(filterSet.getWithYear(null)));
    years.sort((a, b) => a - b);

    const divider = {
      "emissions": 1000000,
      "sales": 1000,
      "population": 1000000,
    }[filterSet.getMetric()];

    const dimensionValues = Array.of(...results.getDimensionValues(filterSet));
    dimensionValues.sort();

    const getForDimValue = (dimValue) => {
      const valsWithUnits = years.map((year) => {
        const subFilterSet = filterSet.getWithYear(year).getWithDimensionValue(dimValue);
        return results.getMetric(subFilterSet);
      });
      const vals = valsWithUnits.map((x) => x.getValue());
      const valsScaled = vals.map((x) => x / divider);
      return {"name": dimValue, "vals": valsScaled};
    };
    const dimensionSeries = dimensionValues.map(getForDimValue);

    const unconstrainedDimValues = Array.of(...results.getDimensionValues(
      filterSet.getWithDimensionValue(null)
    ));
    unconstrainedDimValues.sort();

    const chartJsDatasets = dimensionSeries.map((x) => {
      const index = unconstrainedDimValues.indexOf(x["name"]);
      const color = getColor(index);
      return {
        "label": x["name"],
        "data": x["vals"],
        "fill": false,
        "borderColor": color,
        "backgroundColor": color,
      };
    });

    const chartJsData = {
      "labels": years,
      "datasets": chartJsDatasets,
    };

    const metricSelected = filterSet.getMetric();
    const metricUnits = {
      "emissions": "MtCO2e / yr",
      "sales": "mt / yr",
      "population": "units",
    }[metricSelected];

    const chartJsConfig = {
      "type": "line",
      "data": chartJsData,
      "options": {
        "scales": {
          "y": {
            "min": 0,
            "title": {"text": metricUnits, "display": true},
          },
          "x": {
            "title": {"text": "Year", "display": true},
          },
        },
        "plugins": {
          "tooltip": {
            "callbacks": {
              "title": (x) => "Year " + x[0]["label"],
            },
          },
          "legend": {
            "display": false,
          },
        },
      },
    };

    const container = document.getElementById(self._targetId);
    self._chart = new Chart(container, chartJsConfig);
  }
}


class ResultsPresenter {
  constructor(targetId) {
    const self = this;
    self._root = document.getElementById(targetId);
    self._results = null;
    self._filterSet = new FilterSet(null, null, null, null, "emissions", "simulations");

    const onUpdateFilterSet = (x) => self._onUpdateFilterSet(x);
    self._scorecardPresenter = new ScorecardPresenter("scorecards", onUpdateFilterSet);
    self._dimensionPresenter = new DimensionCardPresenter("dimensions", onUpdateFilterSet);
    self._centerChartPresenter = new CenterChartPresenter("center-chart");

    self.hide();
  }

  hide() {
    const self = this;
    self._root.style.display = "none";
  }

  showResults(results) {
    const self = this;
    self._root.style.display = "block";
    self._results = results;
    self._updateInternally();
  }

  _onUpdateFilterSet(newFilterSet) {
    const self = this;
    self._filterSet = newFilterSet;
    self._updateInternally();
  }

  _updateInternally() {
    const self = this;

    const years = self._results.getYears(self._filterSet.getWithYear(null));
    self._filterSet = self._filterSet.getWithYear(Math.max(...years));

    self._scorecardPresenter.showResults(self._results, self._filterSet);
    self._dimensionPresenter.showResults(self._results, self._filterSet);
    self._centerChartPresenter.showResults(self._results, self._filterSet);
  }
}


export {ResultsPresenter};
