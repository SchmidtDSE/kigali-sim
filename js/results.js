import { FilterSet } from "report_data";

class ScorecardPresenter {

  constructor(targetId) {
    const self = this;
    self._root = document.getElementById(targetId);
  }

  showResults(results, currentYear, filterSet) {
    const self = this;
    
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

    self._updateCard(emissionsScorecard, emissionRounded, currentYear, emissionsSelected);
    self._updateCard(salesScorecard, salesMt, currentYear, salesSelected);
    self._updateCard(equipmentScorecard, kiloEquipment, currentYear, equipmentSelected);
  }

  _updateCard(scorecard, value, currentYear, selected) {
    const self = this;
    self._setText(scorecard.querySelector(".value"), value);
    self._setText(scorecard.querySelector(".current-year"), currentYear);
    
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

}


class DimensionCardPresenter {

  constructor(targetId) {
    const self = this;
    self._root = document.getElementById(targetId);
  }

  showResults(results, currentYear, filterSet) {
    const self = this;
    
    const metricSelected = filterSet.getMetric();
    const metricUnits = {
      "emissions": "tCO2e / yr",
      "sales": "mt / yr",
      "population": "units"
    }[metricSelected];
    self._root.querySelector(".units-tick").innerHTML = metricUnits;
  }

}


class ResultsPresenter {
  constructor(targetId) {
    const self = this;
    self._root = document.getElementById(targetId);
    self._year = null;
    self._scorecardPresenter = new ScorecardPresenter("scorecards");
    self._dimensionPresenter = new DimensionCardPresenter("dimensions");
    self.hide();
  }

  hide() {
    const self = this;
    self._root.style.display = "none";
  }

  showResults(results) {
    const self = this;
    self._root.style.display = "block";
    const filterSet = self._buildFilterSet();
    self._year = Math.max(...results.getYears(filterSet));
    self._scorecardPresenter.showResults(results, self._year, filterSet);
    self._dimensionPresenter.showResults(results, self._year, filterSet);
  }

  _buildFilterSet() {
    const self = this;
    return new FilterSet(null, null, null, null, "emissions", "simulations");
  }
}


export {ResultsPresenter};