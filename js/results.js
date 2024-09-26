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

    self._updateCard(emissionsScorecard, emissionRounded, currentYear);
    self._updateCard(salesScorecard, salesMt, currentYear);
    self._updateCard(equipmentScorecard, kiloEquipment, currentYear);
  }

  _updateCard(scorecard, value, currentYear) {
    const self = this;
    self._setText(scorecard.querySelector(".value"), value);
    self._setText(scorecard.querySelector(".current-year"), currentYear);
  }

  _setText(selection, value) {
    const self = this;
    selection.innerHTML = "";
    const newTextNode = document.createTextNode(value);
    selection.appendChild(newTextNode);
  }

}


class ResultsPresenter {
  constructor(targetId) {
    const self = this;
    self._root = document.getElementById(targetId);
    self._year = null;
    self._scorecardPresenter = new ScorecardPresenter("scorecards");
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
  }

  _buildFilterSet() {
    const self = this;
    return new FilterSet(null, null, null, null);
  }
}


export {ResultsPresenter};