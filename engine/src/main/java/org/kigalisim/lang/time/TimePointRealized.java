package org.kigalisim.lang.time;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Description of a time point.
 *
 * <p>Description of a time point. This may be specified as a specific timepoint like year 5 or year
 * 2025. However, it may also be indicated using a "dynamic cap" which is a value like beginning or
 * onwards which can be interpreted by context.</p>
 */
public class TimePointRealized {

  // If given, specific timepoint like year 5 or year 2025. If empty, use the dynamic cap.
  private final Optional<EngineNumber> pointValue;
  private final Optional<String> dynamicCap;

  /**
   * Create a new timepoint.
   *
   * @param pointValue The specific timepoint value.
   */
  public TimePointRealized(EngineNumber pointValue) {
    this.pointValue = Optional.of(pointValue);
    dynamicCap = Optional.empty();
  }

  /**
   * Create a new timepoint.
   *
   * @param dynamicCap The dynamic cap value.
   */
  public TimePointRealized(String dynamicCap) {
    this.dynamicCap = Optional.of(dynamicCap);
    pointValue = Optional.empty();
  }

  /**
   * Determine if the user requested the dynamic cap value.
   *
   * @return True if the user requested a dynamic cap value like beginning.
   */
  public boolean isDynamicCap() {
    if (dynamicCap.isPresent()) {
      assert pointValue.isEmpty();
      return true;
    } else if (pointValue.isPresent()) {
      assert dynamicCap.isEmpty();
      return false;
    } else {
      throw new IllegalStateException(
          "Encountered time point which has neither dynamic cap nor point value."
      );
    }
  }

  /**
   * Get the specific timepoint value.
   *
   * @return The specific timepoint value.
   * @throws IllegalStateException If the timepoint is not a specific value (it is a dynamic cap).
   */
  public EngineNumber getPointValue() {
    return pointValue.orElseThrow();
  }

  /**
   * Get the dynamic cap value.
   *
   * @return The dynamic cap value.
   * @throws IllegalStateException If the timepoint is not a dynamic cap (it is a specific value).
   */
  public String getDynamicCap() {
    return dynamicCap.orElseThrow();
  }

}
