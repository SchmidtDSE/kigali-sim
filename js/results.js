import { FilterSet } from "report_data";

class ScorecardPresenter {

  constructor(targetId, onUpdateFilterSet) {
    const self = this;
    self._root = document.getElementById(targetId);
    self._filterSet = null;
    self._onUpdateFilterSet = onUpdateFilterSet;
    self._registerEventListeners();
  }

  showResults(results, currentYear, filterSet) {
    const self = this;
    self._filterSet = filterSet;
    
    const emissionsScorecard = self._root.querySelector("#emissions-scorecard");
    const salesScorecard = self._root.querySelector("#sales-scorecard");
    const equipmentScorecard = self._root.querySelector("#equipment-scorecard");

    const emissionsValue = results.getEmissions(filterSet);
    const salesValue = results.getSales(filterSet);
    const equipmentValue = results.getPopulation(filterSet);

    const emissionRounded = Math.round(emissionsValue.getValue());
    const salesMt = Math.round(salesValue.getValue() / 1000);
    const kiloEquipment = Math.round(equipmentValue.getValue() / 1000) + 'k';

    const metricSelected = filterSet.getMetric();
    const emissionsSelected = metricSelected === "emissions";
    const salesSelected = metricSelected === "sales";
    const equipmentSelected = metricSelected === "population";

    const hideVal = !self._filterSet.hasSingleScenario();

    self._updateCard(emissionsScorecard, emissionRounded, currentYear, emissionsSelected, hideVal);
    self._updateCard(salesScorecard, salesMt, currentYear, salesSelected, hideVal);
    self._updateCard(equipmentScorecard, kiloEquipment, currentYear, equipmentSelected, hideVal);
  }

  _updateCard(scorecard, value, currentYear, selected, hideVal) {
    const self = this;
    self._setText(scorecard.querySelector(".value"), value);
    self._setText(scorecard.querySelector(".current-year"), currentYear);

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

  showResults(results, currentYear, filterSet) {
    const self = this;
    self._filterSet = filterSet;
    
    const metricSelected = self._filterSet.getMetric();
    const metricUnits = {
      "emissions": "tCO2e / yr",
      "sales": "mt / yr",
      "population": "units"
    }[metricSelected];
    const allTickUnits = Array.of(...self._root.querySelectorAll(".units-tick"));
    allTickUnits.forEach((x) => x.innerHTML = metricUnits);

    const simulationsCard = self._root.querySelector("#simulations-dimension");
    const applicationsCard = self._root.querySelector("#applications-dimension");
    const substancesCard = self._root.querySelector("#substances-dimension");

    const dimensionSelected = self._filterSet.getDimension();
    const simulationsSelected = dimensionSelected === "simulations";
    const applicationsSelected = dimensionSelected === "applications";
    const substancesSelected = dimensionSelected === "substances";

    self._updateCard(
      "sim",
      simulationsCard,
      results.getScenarios(),
      simulationsSelected,
      self._filterSet.getScenario(),
      (x) => self._filterSet.getWithScenario(x),
      true,
    );
    
    self._updateCard(
      "app",
      applicationsCard,
      results.getApplications(self._filterSet.getWithApplication(null)),
      applicationsSelected,
      self._filterSet.getApplication(),
      (x) => self._filterSet.getWithApplication(x),
      true,
    );
    
    self._updateCard(
      "sub",
      substancesCard,
      results.getSubstances(self._filterSet.getWithSubstance(null)),
      substancesSelected,
      self._filterSet.getSubstance(),
      (x) => self._filterSet.getWithSubstance(x),
      true,
    );
  }

  _updateCard(label, card, identifiers, selected, subSelection, subFilterSetBuilder, addAll) {
    const self = this;
    
    if (selected) {
      card.classList.remove("inactive");
    } else {
      card.classList.add("inactive");
    }

    const identifiersArray = Array.of(...identifiers);
    identifiersArray.sort();

    if (addAll) {
      identifiersArray.unshift("All");
    }

    const itemDivs = d3.select(card).select(".list").selectAll(".item")
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
      .property("checked", (x) => x === subSelection || (subSelection === null && x === "All"));
    
    itemLabels.append("span").text((x) => x);

    itemLabels.on("click", (event, x) => {
      const newFilterSet = subFilterSetBuilder(x === "All" ? null : x);
      self._onUpdateFilterSet(newFilterSet);
    });
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


class ResultsPresenter {
  constructor(targetId) {
    const self = this;
    self._root = document.getElementById(targetId);
    self._year = null;
    self._results = null;
    self._filterSet = new FilterSet(null, null, null, null, "emissions", "simulations");
    
    const onUpdateFilterSet = (x) => self._onUpdateFilterSet(x);
    self._scorecardPresenter = new ScorecardPresenter("scorecards", onUpdateFilterSet);
    self._dimensionPresenter = new DimensionCardPresenter("dimensions", onUpdateFilterSet);
    
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

    const years = self._results.getYears(self._filterSet);
    if (!years.has(self._year)) {
      self._year = Math.max(...years);
    }

    self._scorecardPresenter.showResults(self._results, self._year, self._filterSet);
    self._dimensionPresenter.showResults(self._results, self._year, self._filterSet);
  }
}


export {ResultsPresenter};