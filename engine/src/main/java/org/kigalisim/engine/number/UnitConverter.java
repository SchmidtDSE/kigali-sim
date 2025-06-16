/**
 * Unit conversion logic for the engine.
 *
 * <p>This class provides functionality to convert between different units used in the
 * simulation engine, including volume, population, consumption, and time-based units.
 * It uses BigDecimal for numerical precision and stability.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.number;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

/**
 * Object simplifying conversion between units.
 *
 * <p>This class handles unit conversions within the engine, supporting conversions
 * between volume units (kg, mt), population units (unit, units), consumption units
 * (tCO2e, kwh), time units (year, years), and percentage units (%).</p>
 */
public class UnitConverter {

  // Configuration constants
  private static final boolean CONVERT_ZERO_NOOP = true;
  private static final boolean ZERO_EMPTY_VOLUME_INTENSITY = false;

  // Math context for BigDecimal operations
  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

  // Conversion factors
  private static final BigDecimal KG_TO_MT_FACTOR = new BigDecimal("1000");
  private static final BigDecimal PERCENT_FACTOR = new BigDecimal("100");

  private final StateGetter stateGetter;

  /**
   * Create a new unit converter.
   *
   * @param stateGetter Object allowing access to engine state as needed for unit conversion
   */
  public UnitConverter(StateGetter stateGetter) {
    this.stateGetter = stateGetter;
  }

  /**
   * Convert a number to new units.
   *
   * @param source The EngineNumber to convert
   * @param destinationUnits The units to which source should be converted
   * @return The converted EngineNumber
   */
  public EngineNumber convert(EngineNumber source, String destinationUnits) {
    if (source.getUnits().equals(destinationUnits)) {
      return source;
    }

    if (CONVERT_ZERO_NOOP && source.getValue().compareTo(BigDecimal.ZERO) == 0) {
      return new EngineNumber(BigDecimal.ZERO, destinationUnits);
    }

    String[] sourceUnitPieces = source.getUnits().split(" / ");
    boolean sourceHasDenominator = sourceUnitPieces.length > 1;
    String sourceDenominatorUnits = sourceHasDenominator ? sourceUnitPieces[1] : "";

    String[] destinationUnitPieces = destinationUnits.split(" / ");
    boolean destHasDenominator = destinationUnitPieces.length > 1;
    String destinationDenominatorUnits = destHasDenominator ? destinationUnitPieces[1] : "";

    String sourceNumeratorUnits = sourceUnitPieces[0];
    String destinationNumeratorUnits = destinationUnitPieces[0];
    boolean differentDenominator = !destinationDenominatorUnits.equals(sourceDenominatorUnits);
    boolean sameDenominator = !differentDenominator;

    // Create strategy maps
    Map<String, UnitConverterStrategy> numeratorStrategy =
        createNumeratorStrategy();
    Map<String, UnitConverterStrategy> denominatorStrategy =
        createDenominatorStrategy();

    UnitConverterStrategy numeratorFunc =
        numeratorStrategy.get(destinationNumeratorUnits);
    UnitConverterStrategy denominatorFunc =
        denominatorStrategy.get(destinationDenominatorUnits);

    if (numeratorFunc == null) {
      throw new IllegalArgumentException(
          "Unsupported destination numerator units: " + destinationNumeratorUnits);
    }
    if (denominatorFunc == null) {
      throw new IllegalArgumentException(
          "Unsupported destination denominator units: " + destinationDenominatorUnits);
    }

    if (sourceHasDenominator && sameDenominator) {
      EngineNumber sourceEffective = new EngineNumber(source.getValue(), sourceNumeratorUnits);
      EngineNumber convertedNumerator = numeratorFunc.convert(sourceEffective);
      return new EngineNumber(convertedNumerator.getValue(), destinationUnits);
    } else {
      EngineNumber numerator = numeratorFunc.convert(source);
      EngineNumber denominator = denominatorFunc.convert(source);

      if (denominator.getValue().compareTo(BigDecimal.ZERO) == 0) {
        BigDecimal inferredFactor = inferScale(sourceDenominatorUnits,
            destinationDenominatorUnits);
        if (inferredFactor != null) {
          return new EngineNumber(
              numerator.getValue().divide(inferredFactor, MATH_CONTEXT), destinationUnits);
        } else if (ZERO_EMPTY_VOLUME_INTENSITY) {
          return new EngineNumber(BigDecimal.ZERO, destinationUnits);
        } else {
          throw new RuntimeException(
              "Encountered unrecoverable NaN in conversion due to no volume.");
        }
      } else {
        return new EngineNumber(
            numerator.getValue().divide(denominator.getValue(), MATH_CONTEXT), destinationUnits);
      }
    }
  }

  /**
   * Create the numerator conversion strategy map.
   *
   * @return Map of unit types to conversion strategies
   */
  private Map<String, UnitConverterStrategy> createNumeratorStrategy() {
    Map<String, UnitConverterStrategy> strategy = new HashMap<>();
    strategy.put("kg", this::toKg);
    strategy.put("mt", this::toMt);
    strategy.put("unit", this::toUnits);
    strategy.put("units", this::toUnits);
    strategy.put("tCO2e", this::toGhgConsumption);
    strategy.put("kwh", this::toEnergyConsumption);
    strategy.put("year", this::toYears);
    strategy.put("years", this::toYears);
    strategy.put("%", this::toPercent);
    return strategy;
  }

  /**
   * Create the denominator conversion strategy map.
   *
   * @return Map of unit types to conversion strategies
   */
  private Map<String, UnitConverterStrategy> createDenominatorStrategy() {
    Map<String, UnitConverterStrategy> strategy = new HashMap<>();
    strategy.put("kg", x -> convert(stateGetter.getVolume(), "kg"));
    strategy.put("mt", x -> convert(stateGetter.getVolume(), "mt"));
    strategy.put("unit", x -> convert(stateGetter.getPopulation(), "unit"));
    strategy.put("units", x -> convert(stateGetter.getPopulation(), "units"));
    strategy.put("tCO2e", x -> convert(stateGetter.getGhgConsumption(), "tCO2e"));
    strategy.put("kwh", x -> convert(stateGetter.getEnergyConsumption(), "kwh"));
    strategy.put("year", x -> convert(stateGetter.getYearsElapsed(), "year"));
    strategy.put("years", x -> convert(stateGetter.getYearsElapsed(), "years"));
    strategy.put("", x -> new EngineNumber(BigDecimal.ONE, ""));
    return strategy;
  }

  /**
   * Infer a scaling factor without population information.
   *
   * @param source The source unit type
   * @param destination The destination unit type
   * @return The scale factor for conversion or null if not found
   */
  private BigDecimal inferScale(String source, String destination) {
    Map<String, Map<String, BigDecimal>> scaleMap = new HashMap<>();

    Map<String, BigDecimal> kgScales = new HashMap<>();
    kgScales.put("mt", KG_TO_MT_FACTOR);
    scaleMap.put("kg", kgScales);

    Map<String, BigDecimal> mtScales = new HashMap<>();
    mtScales.put("kg", BigDecimal.ONE.divide(KG_TO_MT_FACTOR, MATH_CONTEXT));
    scaleMap.put("mt", mtScales);

    Map<String, BigDecimal> unitScales = new HashMap<>();
    unitScales.put("units", BigDecimal.ONE);
    scaleMap.put("unit", unitScales);

    Map<String, BigDecimal> unitsScales = new HashMap<>();
    unitsScales.put("unit", BigDecimal.ONE);
    scaleMap.put("units", unitsScales);

    Map<String, BigDecimal> yearsScales = new HashMap<>();
    yearsScales.put("year", BigDecimal.ONE);
    scaleMap.put("years", yearsScales);

    Map<String, BigDecimal> yearScales = new HashMap<>();
    yearScales.put("years", BigDecimal.ONE);
    scaleMap.put("year", yearScales);

    Map<String, BigDecimal> sourceScales = scaleMap.get(source);
    if (sourceScales != null) {
      return sourceScales.get(destination);
    }
    return null;
  }

  /**
   * Convert a number to kilograms.
   *
   * @param target The EngineNumber to convert
   * @return Target converted to kilograms
   */
  private EngineNumber toKg(EngineNumber target) {
    EngineNumber asVolume = toVolume(target);
    String currentUnits = asVolume.getUnits();
    if ("mt".equals(currentUnits)) {
      return new EngineNumber(asVolume.getValue().multiply(KG_TO_MT_FACTOR), "kg");
    } else if ("kg".equals(currentUnits)) {
      return asVolume;
    } else {
      throw new IllegalArgumentException("Unexpected units " + currentUnits);
    }
  }

  /**
   * Convert a number to metric tons.
   *
   * @param target The EngineNumber to convert
   * @return Target converted to metric tons
   */
  private EngineNumber toMt(EngineNumber target) {
    EngineNumber asVolume = toVolume(target);
    String currentUnits = asVolume.getUnits();
    if ("kg".equals(currentUnits)) {
      return new EngineNumber(asVolume.getValue().divide(KG_TO_MT_FACTOR, MATH_CONTEXT), "mt");
    } else if ("mt".equals(currentUnits)) {
      return asVolume;
    } else {
      throw new IllegalArgumentException("Unexpected units " + currentUnits);
    }
  }

  /**
   * Convert a number to volume units.
   *
   * @param target The EngineNumber to convert
   * @return Target converted to kilograms or metric tons
   */
  private EngineNumber toVolume(EngineNumber target) {
    target = normalize(target);
    String currentUnits = target.getUnits();

    if ("mt".equals(currentUnits) || "kg".equals(currentUnits)) {
      return target;
    } else if ("tCO2e".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      EngineNumber conversion = stateGetter.getSubstanceConsumption();
      BigDecimal conversionValue = conversion.getValue();
      String newUnits = conversion.getUnits().split(" / ")[1];
      BigDecimal newValue = originalValue.divide(conversionValue, MATH_CONTEXT);
      return new EngineNumber(newValue, newUnits);
    } else if ("unit".equals(currentUnits) || "units".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      EngineNumber conversion = stateGetter.getAmortizedUnitVolume();
      BigDecimal conversionValue = conversion.getValue();
      String newUnits = conversion.getUnits().split(" / ")[0];
      BigDecimal newValue = originalValue.multiply(conversionValue);
      return new EngineNumber(newValue, newUnits);
    } else if ("%".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      BigDecimal asRatio = originalValue.divide(PERCENT_FACTOR, MATH_CONTEXT);
      EngineNumber total = stateGetter.getVolume();
      String newUnits = total.getUnits();
      BigDecimal newValue = total.getValue().multiply(asRatio);
      return new EngineNumber(newValue, newUnits);
    } else {
      throw new IllegalArgumentException("Unable to convert to volume: " + currentUnits);
    }
  }

  /**
   * Convert a number to units (population).
   *
   * @param target The EngineNumber to convert
   * @return Target converted to units (population)
   */
  private EngineNumber toUnits(EngineNumber target) {
    target = normalize(target);
    String currentUnits = target.getUnits();

    if ("units".equals(currentUnits)) {
      return target;
    } else if ("unit".equals(currentUnits)) {
      return new EngineNumber(target.getValue(), "units");
    } else if ("kg".equals(currentUnits) || "mt".equals(currentUnits)) {
      EngineNumber conversion = stateGetter.getAmortizedUnitVolume();
      BigDecimal conversionValue = conversion.getValue();
      String[] conversionUnitPieces = conversion.getUnits().split(" / ");
      String expectedUnits = conversionUnitPieces[0];
      EngineNumber targetConverted = convert(target, expectedUnits);
      BigDecimal originalValue = targetConverted.getValue();
      BigDecimal newValue = originalValue.divide(conversionValue, MATH_CONTEXT);
      return new EngineNumber(newValue, "units");
    } else if ("tCO2e".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      EngineNumber conversion = stateGetter.getAmortizedUnitConsumption();
      BigDecimal conversionValue = conversion.getValue();
      BigDecimal newValue = originalValue.divide(conversionValue, MATH_CONTEXT);
      return new EngineNumber(newValue, "units");
    } else if ("%".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      BigDecimal asRatio = originalValue.divide(PERCENT_FACTOR, MATH_CONTEXT);
      EngineNumber total = stateGetter.getPopulation();
      BigDecimal newValue = total.getValue().multiply(asRatio);
      return new EngineNumber(newValue, "units");
    } else {
      throw new IllegalArgumentException("Unable to convert to population: " + currentUnits);
    }
  }

  /**
   * Convert a number to consumption as tCO2e.
   *
   * @param target The EngineNumber to convert
   * @return Target converted to consumption as tCO2e
   */
  private EngineNumber toGhgConsumption(EngineNumber target) {
    target = normalize(target);
    String currentUnits = target.getUnits();

    boolean currentVolume = "kg".equals(currentUnits) || "mt".equals(currentUnits);
    boolean currentPop = "unit".equals(currentUnits) || "units".equals(currentUnits);
    boolean currentInfer = currentVolume || currentPop;

    if ("tCO2e".equals(currentUnits)) {
      return target;
    } else if (currentInfer) {
      EngineNumber conversion = stateGetter.getSubstanceConsumption();
      BigDecimal conversionValue = conversion.getValue();
      String[] conversionUnitPieces = conversion.getUnits().split(" / ");
      String newUnits = conversionUnitPieces[0];
      String expectedUnits = conversionUnitPieces[1];
      EngineNumber targetConverted = convert(target, expectedUnits);
      BigDecimal originalValue = targetConverted.getValue();
      BigDecimal newValue = originalValue.multiply(conversionValue);
      if (!"tCO2e".equals(newUnits)) {
        throw new IllegalArgumentException("Unexpected units " + newUnits);
      }
      return new EngineNumber(newValue, newUnits);
    } else if ("%".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      BigDecimal asRatio = originalValue.divide(PERCENT_FACTOR, MATH_CONTEXT);
      EngineNumber total = stateGetter.getGhgConsumption();
      BigDecimal newValue = total.getValue().multiply(asRatio);
      return new EngineNumber(newValue, "tCO2e");
    } else {
      throw new IllegalArgumentException("Unable to convert to consumption: " + currentUnits);
    }
  }

  /**
   * Convert a number to energy consumption as kwh.
   *
   * @param target The EngineNumber to convert
   * @return Target converted to energy consumption as kwh
   */
  private EngineNumber toEnergyConsumption(EngineNumber target) {
    target = normalize(target);
    String currentUnits = target.getUnits();

    boolean currentVolume = "kg".equals(currentUnits) || "mt".equals(currentUnits);
    boolean currentPop = "unit".equals(currentUnits) || "units".equals(currentUnits);
    boolean currentInfer = currentVolume || currentPop;

    if ("kwh".equals(currentUnits)) {
      return target;
    } else if (currentInfer) {
      EngineNumber conversion = stateGetter.getEnergyIntensity();
      BigDecimal conversionValue = conversion.getValue();
      String[] conversionUnitPieces = conversion.getUnits().split(" / ");
      String newUnits = conversionUnitPieces[0];
      String expectedUnits = conversionUnitPieces[1];
      EngineNumber targetConverted = convert(target, expectedUnits);
      BigDecimal originalValue = targetConverted.getValue();
      BigDecimal newValue = originalValue.multiply(conversionValue);
      if (!"kwh".equals(newUnits)) {
        throw new IllegalArgumentException("Unexpected units " + newUnits);
      }
      return new EngineNumber(newValue, newUnits);
    } else if ("%".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      BigDecimal asRatio = originalValue.divide(PERCENT_FACTOR, MATH_CONTEXT);
      EngineNumber total = stateGetter.getEnergyConsumption();
      BigDecimal newValue = total.getValue().multiply(asRatio);
      return new EngineNumber(newValue, "kwh");
    } else {
      throw new IllegalArgumentException(
          "Unable to convert to energy consumption: " + currentUnits);
    }
  }

  /**
   * Convert a number to years.
   *
   * @param target The EngineNumber to convert
   * @return Target converted to years
   */
  private EngineNumber toYears(EngineNumber target) {
    target = normalize(target);
    String currentUnits = target.getUnits();

    if ("years".equals(currentUnits)) {
      return target;
    } else if ("year".equals(currentUnits)) {
      return new EngineNumber(target.getValue(), "years");
    } else if ("tCO2e".equals(currentUnits)) {
      BigDecimal perYearConsumptionValue = stateGetter.getGhgConsumption().getValue();
      BigDecimal newYears = target.getValue().divide(perYearConsumptionValue, MATH_CONTEXT);
      return new EngineNumber(newYears, "years");
    } else if ("kwh".equals(currentUnits)) {
      BigDecimal perYearConsumptionValue = stateGetter.getEnergyConsumption().getValue();
      BigDecimal newYears = target.getValue().divide(perYearConsumptionValue, MATH_CONTEXT);
      return new EngineNumber(newYears, "years");
    } else if ("kg".equals(currentUnits) || "mt".equals(currentUnits)) {
      EngineNumber perYearVolume = stateGetter.getVolume();
      String perYearVolumeUnits = perYearVolume.getUnits();
      BigDecimal perYearVolumeValue = perYearVolume.getValue();
      EngineNumber volumeConverted = convert(target, perYearVolumeUnits);
      BigDecimal volumeConvertedValue = volumeConverted.getValue();
      BigDecimal newYears = volumeConvertedValue.divide(perYearVolumeValue, MATH_CONTEXT);
      return new EngineNumber(newYears, "years");
    } else if ("unit".equals(currentUnits) || "units".equals(currentUnits)) {
      BigDecimal perYearPopulation = stateGetter.getPopulationChange().getValue();
      BigDecimal newYears = target.getValue().divide(perYearPopulation, MATH_CONTEXT);
      return new EngineNumber(newYears, "years");
    } else if ("%".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      BigDecimal asRatio = originalValue.divide(PERCENT_FACTOR, MATH_CONTEXT);
      EngineNumber total = stateGetter.getYearsElapsed();
      BigDecimal newValue = total.getValue().multiply(asRatio);
      return new EngineNumber(newValue, "years");
    } else {
      throw new IllegalArgumentException("Unable to convert to years: " + currentUnits);
    }
  }

  /**
   * Convert a number to percentage.
   *
   * @param target The EngineNumber to convert
   * @return Target converted to percentage
   */
  private EngineNumber toPercent(EngineNumber target) {
    target = normalize(target);
    String currentUnits = target.getUnits();

    EngineNumber total;
    if ("years".equals(currentUnits) || "year".equals(currentUnits)) {
      total = stateGetter.getYearsElapsed();
    } else if ("tCO2e".equals(currentUnits)) {
      total = stateGetter.getGhgConsumption();
    } else if ("kg".equals(currentUnits) || "mt".equals(currentUnits)) {
      EngineNumber volume = stateGetter.getVolume();
      total = convert(volume, currentUnits);
    } else if ("unit".equals(currentUnits) || "units".equals(currentUnits)) {
      total = stateGetter.getPopulation();
    } else {
      throw new IllegalArgumentException(
          "Unable to convert to %: " + currentUnits);
    }

    BigDecimal percentValue = target.getValue()
        .divide(total.getValue(), MATH_CONTEXT)
        .multiply(PERCENT_FACTOR);
    return new EngineNumber(percentValue, "%");
  }

  /**
   * Normalize to non-ratio units if possible.
   *
   * @param target The number to convert from a units with ratio to single type units
   * @return Number after conversion to non-ratio units or target unchanged if
   *     it does not have a ratio units or could not be normalized
   */
  private EngineNumber normalize(EngineNumber target) {
    target = normUnits(target);
    target = normTime(target);
    target = normConsumption(target);
    target = normVolume(target);
    return target;
  }

  /**
   * Convert a number where a units ratio has population in the denominator to a
   * non-ratio units.
   *
   * @param target The value to normalize by population
   * @return Target without population in its units denominator
   */
  private EngineNumber normUnits(EngineNumber target) {
    String currentUnits = target.getUnits();

    boolean divUnit = currentUnits.endsWith("/ unit");
    boolean divUnits = currentUnits.endsWith("/ units");
    boolean isPerUnit = divUnit || divUnits;

    if (!isPerUnit) {
      return target;
    }

    BigDecimal originalValue = target.getValue();
    String newUnits = currentUnits.split(" / ")[0];
    EngineNumber population = stateGetter.getPopulation();
    BigDecimal populationValue = population.getValue();
    BigDecimal newValue = originalValue.multiply(populationValue);

    return new EngineNumber(newValue, newUnits);
  }

  /**
   * Convert a number where a units ratio has time in the denominator to a non-ratio units.
   *
   * @param target The value to normalize by time
   * @return Target without time in its units denominator
   */
  private EngineNumber normTime(EngineNumber target) {
    String currentUnits = target.getUnits();

    if (!currentUnits.endsWith(" / year")) {
      return target;
    }

    BigDecimal originalValue = target.getValue();
    String newUnits = currentUnits.split(" / ")[0];
    EngineNumber years = stateGetter.getYearsElapsed();
    BigDecimal yearsValue = years.getValue();
    BigDecimal newValue = originalValue.multiply(yearsValue);

    return new EngineNumber(newValue, newUnits);
  }

  /**
   * Convert a number where a units ratio has consumption in the denominator to a
   * non-ratio units.
   *
   * @param target The value to normalize by consumption
   * @return Target without consumption in its units denominator
   */
  private EngineNumber normConsumption(EngineNumber target) {
    String currentUnits = target.getUnits();

    boolean isCo2 = currentUnits.endsWith(" / tCO2e");
    boolean isKwh = currentUnits.endsWith(" / kwh");
    if (!isCo2 && !isKwh) {
      return target;
    }

    EngineNumber targetConsumption;
    if (isCo2) {
      targetConsumption = stateGetter.getGhgConsumption();
    } else {
      targetConsumption = stateGetter.getEnergyConsumption();
    }

    BigDecimal originalValue = target.getValue();
    String newUnits = currentUnits.split(" / ")[0];
    BigDecimal totalConsumptionValue = targetConsumption.getValue();
    BigDecimal newValue = originalValue.multiply(totalConsumptionValue);

    return new EngineNumber(newValue, newUnits);
  }

  /**
   * Convert a number where a units ratio has volume in the denominator to a non-ratio units.
   *
   * @param target The value to normalize by volume
   * @return Target without volume in its units denominator
   */
  private EngineNumber normVolume(EngineNumber target) {
    String targetUnits = target.getUnits();

    boolean divKg = targetUnits.endsWith(" / kg");
    boolean divMt = targetUnits.endsWith(" / mt");
    boolean needsNorm = divKg || divMt;
    if (!needsNorm) {
      return target;
    }

    String[] targetUnitPieces = targetUnits.split(" / ");
    String newUnits = targetUnitPieces[0];
    String expectedUnits = targetUnitPieces[1];

    EngineNumber volume = stateGetter.getVolume();
    EngineNumber volumeConverted = convert(volume, expectedUnits);
    BigDecimal conversionValue = volumeConverted.getValue();

    BigDecimal originalValue = target.getValue();
    BigDecimal newValue = originalValue.multiply(conversionValue);

    return new EngineNumber(newValue, newUnits);
  }
}
