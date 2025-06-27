/**
 * Year range matching utility.
 *
 * @license BSD, see LICENSE.md.
 */

/**
 * Class representing a range of years where inclusion can be tested.
 */
class YearMatcher {
  /**
   * Create a new year range.
   *
   * Create a new year range between start and end where null in either means positive or negative
   * infinity.
   *
   * @param start The starting year (inclusive) in this range or null if no min year.
   * @param end The ending year (inclusive) in this range or null if no max year.
   */
  constructor(start, end) {
    const self = this;

    const hasNull = start === null || end === null;
    const startHasSpecial = start === "beginning" || start === "onwards";
    const endHasSpecial = end === "beginning" || end === "onwards";

    if (hasNull || startHasSpecial || endHasSpecial) {
      self._start = start;
      self._end = end;
    } else {
      const startRearrange = Math.min(start, end);
      const endRearrange = Math.max(start, end);

      self._start = startRearrange;
      self._end = endRearrange;
    }
  }

  /**
   * Determine if a year is included in this range.
   *
   * @param year The year to test for inclusion.
   * @returns True if this value is between getStart and getEnd.
   */
  getInRange(year) {
    const self = this;
    const meetsMin = self._start === null || self._start <= year;
    const meetsMax = self._end === null || self._end >= year;
    return meetsMin && meetsMax;
  }

  /**
   * Get the start of the year range.
   *
   * @returns The minimum included year in this range or null if negative infinity.
   */
  getStart() {
    const self = this;
    return self._start;
  }

  /**
   * Get the end of the year range.
   *
   * @returns The maximum included year in this range or null if positive infinity.
   */
  getEnd() {
    const self = this;
    return self._end;
  }
}

export {YearMatcher};
