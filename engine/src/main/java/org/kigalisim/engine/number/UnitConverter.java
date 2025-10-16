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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.kigalisim.engine.state.StateGetter;
import org.kigalisim.util.UnitStringNormalizer;

/**
 * Object simplifying conversion between units.
 *
 * <p>This class handles unit conversions within the engine, supporting conversions
 * between volume units (kg, mt), population units (unit, units), consumption units
 * (tCO2e, kgCO2e, kwh), time units (year, years), and percentage units (%).</p>
 */
public class UnitConverter {

  // Configuration constants
  private static final boolean CONVERT_ZERO_NOOP = true;
  private static final boolean ZERO_EMPTY_VOLUME_INTENSITY = true;

  // Math context for BigDecimal operations
  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

  // Conversion factors
  private static final BigDecimal KG_TO_MT_FACTOR = new BigDecimal("1000");
  private static final BigDecimal MT_TO_KG_FACTOR =
      BigDecimal.ONE.divide(KG_TO_MT_FACTOR, MATH_CONTEXT);
  private static final BigDecimal PERCENT_FACTOR = new BigDecimal("100");
  private static final BigDecimal TCO2E_TO_KGCO2E_FACTOR = new BigDecimal("1000");
  // Pre-calculated inverse conversion factors for multiplication instead of division
  // Following the pattern of MT_TO_KG_FACTOR optimization (2% speedup)
  private static final BigDecimal KGCO2E_TO_TCO2E_FACTOR =
      BigDecimal.ONE.divide(TCO2E_TO_KGCO2E_FACTOR, MATH_CONTEXT);
  private static final BigDecimal TO_PERCENT_FACTOR =
      BigDecimal.ONE.divide(PERCENT_FACTOR, MATH_CONTEXT);

  // Cached scale map for inferScale - initialized once at class load time
  private static final Map<String, Map<String, BigDecimal>> SCALE_MAP = createScaleMap();

  // Cache for state-independent conversion results
  // Key format: "sourceUnits|destUnits|value"
  private static final Map<String, EngineNumber> CONVERSION_CACHE =
      new ConcurrentHashMap<>(256);
  private static final int MAX_CACHE_SIZE = 1000;

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
   * Create and initialize the scale map for unit conversions.
   *
   * <p>This method is called once at class initialization time to create an immutable
   * map of conversion factors between compatible units. The map is cached to avoid
   * repeated allocations during unit conversions.</p>
   *
   * @return An unmodifiable map of conversion scales
   */
  private static Map<String, Map<String, BigDecimal>> createScaleMap() {
    Map<String, Map<String, BigDecimal>> scaleMap = new HashMap<>();

    Map<String, BigDecimal> kgScales = new HashMap<>();
    kgScales.put("mt", KG_TO_MT_FACTOR);
    scaleMap.put("kg", kgScales);

    Map<String, BigDecimal> mtScales = new HashMap<>();
    mtScales.put("kg", MT_TO_KG_FACTOR);
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
    yearScales.put("yr", BigDecimal.ONE);
    yearScales.put("yrs", BigDecimal.ONE);
    scaleMap.put("year", yearScales);

    Map<String, BigDecimal> yrScales = new HashMap<>();
    yrScales.put("year", BigDecimal.ONE);
    yrScales.put("years", BigDecimal.ONE);
    yrScales.put("yrs", BigDecimal.ONE);
    scaleMap.put("yr", yrScales);

    Map<String, BigDecimal> yrsScales = new HashMap<>();
    yrsScales.put("year", BigDecimal.ONE);
    yrsScales.put("years", BigDecimal.ONE);
    yrsScales.put("yr", BigDecimal.ONE);
    scaleMap.put("yrs", yrsScales);

    return Collections.unmodifiableMap(scaleMap);
  }

  /**
   * Check if a conversion is state-independent and can be cached.
   *
   * <p>Only simple conversions that don't depend on StateGetter can be cached:
   * <ul>
   * <li>kg ↔ mt conversions</li>
   * <li>tCO2e ↔ kgCO2e conversions</li>
   * <li>Unit aliases (unit/units, year/years/yr/yrs)</li>
   * </ul>
   *
   * @param sourceUnits The source units (normalized)
   * @param destUnits The destination units (normalized)
   * @return True if this conversion can be cached
   */
  private static boolean isCacheable(String sourceUnits, String destUnits) {
    // kg ↔ mt conversions
    boolean isVolume = ("kg".equals(sourceUnits) || "mt".equals(sourceUnits))
        && ("kg".equals(destUnits) || "mt".equals(destUnits));

    // tCO2e ↔ kgCO2e conversions
    boolean isEmissions = ("tCO2e".equals(sourceUnits) || "kgCO2e".equals(sourceUnits))
        && ("tCO2e".equals(destUnits) || "kgCO2e".equals(destUnits));

    // Unit aliases
    boolean isUnitAlias = ("unit".equals(sourceUnits) || "units".equals(sourceUnits))
        && ("unit".equals(destUnits) || "units".equals(destUnits));

    // Year aliases
    boolean isYearAlias = ("year".equals(sourceUnits) || "years".equals(sourceUnits)
        || "yr".equals(sourceUnits) || "yrs".equals(sourceUnits))
        && ("year".equals(destUnits) || "years".equals(destUnits)
        || "yr".equals(destUnits) || "yrs".equals(destUnits));

    return isVolume || isEmissions || isUnitAlias || isYearAlias;
  }

  /**
   * Create a cache key for a conversion.
   *
   * @param sourceUnits The source units
   * @param destUnits The destination units
   * @param value The value to convert
   * @return Cache key string
   */
  private static String createCacheKey(String sourceUnits, String destUnits, BigDecimal value) {
    return sourceUnits + "|" + destUnits + "|" + value.toPlainString();
  }

  /**
   * Remove all spaces from a unit string.
   *
   * @param unitString The unit string to normalize
   * @return The normalized unit string with all spaces removed
   */
  private static String normalizeUnitString(String unitString) {
    return UnitStringNormalizer.normalize(unitString);
  }

  /**
   * Split a unit string on the first '/' character.
   *
   * <p>This is faster than split("/") since we know there's only one slash.
   * Returns an array with the numerator at [0] and denominator at [1] (or empty string if no slash).</p>
   *
   * @param unitString The unit string to split
   * @return Array with [numerator, denominator]
   */
  private static String[] splitUnits(String unitString) {
    int slashIndex = unitString.indexOf('/');
    if (slashIndex == -1) {
      return new String[]{unitString, ""};
    }
    return new String[]{
      unitString.substring(0, slashIndex),
      unitString.substring(slashIndex + 1)
    };
  }

  /**
   * Get the numerator part of a unit string (before the '/').
   *
   * @param unitString The unit string
   * @return The numerator part
   */
  private static String getNumerator(String unitString) {
    int slashIndex = unitString.indexOf('/');
    return slashIndex == -1 ? unitString : unitString.substring(0, slashIndex);
  }

  /**
   * Check if a unit string represents a years unit.
   *
   * @param unitString The unit string to check
   * @return True if the unit represents years, false otherwise
   */
  private boolean isYearsUnitStr(String unitString) {
    return switch (unitString) {
      case "year", "years", "yr", "yrs" -> true;
      default -> false;
    };
  }

  /**
   * Check if a normalized unit string ends with a per-year denominator.
   *
   * @param normalizedUnits The normalized unit string to check
   * @return True if the unit ends with per-year denominator, false otherwise
   */
  private boolean getEndsWithPerYear(String normalizedUnits) {
    return normalizedUnits.endsWith("/year")
        || normalizedUnits.endsWith("/yr")
        || normalizedUnits.endsWith("/yrs");
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
    } else if (CONVERT_ZERO_NOOP && source.getValue().compareTo(BigDecimal.ZERO) == 0) {
      return new EngineNumber(BigDecimal.ZERO, destinationUnits);
    } else {
      String normalizedSourceUnits = normalizeUnitString(source.getUnits());
      String normalizedDestinationUnits = normalizeUnitString(destinationUnits);

      // Check cache for state-independent conversions
      boolean cacheable = isCacheable(normalizedSourceUnits, normalizedDestinationUnits);
      String cacheKey = null;
      if (cacheable) {
        cacheKey = createCacheKey(normalizedSourceUnits, normalizedDestinationUnits,
            source.getValue());
        EngineNumber cached = CONVERSION_CACHE.get(cacheKey);
        if (cached != null) {
          return cached;
        }
      }

      String[] sourceUnitPieces = splitUnits(normalizedSourceUnits);
      boolean sourceHasDenominator = sourceUnitPieces.length > 1;
      String sourceDenominatorUnits = sourceHasDenominator ? sourceUnitPieces[1] : "";

      String[] destinationUnitPieces = splitUnits(normalizedDestinationUnits);
      boolean destHasDenominator = destinationUnitPieces.length > 1;
      String destinationDenominatorUnits = destHasDenominator ? destinationUnitPieces[1] : "";

      String sourceNumeratorUnits = sourceUnitPieces[0];
      String destinationNumeratorUnits = destinationUnitPieces[0];
      boolean differentDenominator = !destinationDenominatorUnits.equals(sourceDenominatorUnits);
      boolean sameDenominator = !differentDenominator;

      EngineNumber result;
      if (sourceHasDenominator && sameDenominator) {
        EngineNumber sourceEffective = new EngineNumber(source.getValue(), sourceNumeratorUnits);
        EngineNumber convertedNumerator = convertNumerator(sourceEffective, destinationNumeratorUnits);
        result = new EngineNumber(convertedNumerator.getValue(), destinationUnits);
      } else {
        EngineNumber numerator = convertNumerator(source, destinationNumeratorUnits);
        EngineNumber denominator = convertDenominator(source, destinationDenominatorUnits);

        if (denominator.getValue().compareTo(BigDecimal.ZERO) == 0) {
          BigDecimal inferredFactor = inferScale(sourceDenominatorUnits,
              destinationDenominatorUnits);
          if (inferredFactor != null) {
            result = new EngineNumber(
                numerator.getValue().divide(inferredFactor, MATH_CONTEXT), destinationUnits);
          } else if (ZERO_EMPTY_VOLUME_INTENSITY) {
            result = new EngineNumber(BigDecimal.ZERO, destinationUnits);
          } else {
            throw new RuntimeException(
                "Encountered unrecoverable NaN in conversion due to no volume.");
          }
        } else {
          result = new EngineNumber(
              numerator.getValue().divide(denominator.getValue(), MATH_CONTEXT), destinationUnits);
        }
      }

      // Cache the result if it's cacheable and cache isn't too full
      if (cacheable && CONVERSION_CACHE.size() < MAX_CACHE_SIZE) {
        CONVERSION_CACHE.put(cacheKey, result);
      }

      return result;
    }
  }

  /**
   * Convert a number to the specified numerator units.
   *
   * @param input The EngineNumber to convert
   * @param destinationUnits The target numerator units
   * @return The converted EngineNumber
   */
  private EngineNumber convertNumerator(EngineNumber input, String destinationUnits) {
    return switch (destinationUnits) {
      case "kg" -> toKg(input);
      case "mt" -> toMt(input);
      case "unit", "units" -> toUnits(input);
      case "tCO2e" -> toTonnesCo2eConsumption(input);
      case "kgCO2e" -> toKgCo2eConsumption(input);
      case "kwh" -> toEnergyConsumption(input);
      case "year", "years", "yr", "yrs" -> toYears(input);
      case "%" -> toPercent(input);
      default -> throw new IllegalArgumentException(
          "Unsupported destination numerator units: " + destinationUnits);
    };
  }

  /**
   * Convert a number to the specified denominator units.
   *
   * @param input The EngineNumber to convert (not used for denominator conversions)
   * @param destinationUnits The target denominator units
   * @return The converted EngineNumber representing the denominator
   */
  private EngineNumber convertDenominator(EngineNumber input, String destinationUnits) {
    return switch (destinationUnits) {
      case "kg" -> convert(stateGetter.getVolume(), "kg");
      case "mt" -> convert(stateGetter.getVolume(), "mt");
      case "unit", "units" -> convert(stateGetter.getPopulation(), destinationUnits);
      case "tCO2e" -> convert(stateGetter.getGhgConsumption(), "tCO2e");
      case "kgCO2e" -> convert(stateGetter.getGhgConsumption(), "kgCO2e");
      case "kwh" -> convert(stateGetter.getEnergyConsumption(), "kwh");
      case "year", "years", "yr", "yrs" -> convert(stateGetter.getYearsElapsed(), destinationUnits);
      case "" -> new EngineNumber(BigDecimal.ONE, "");
      default -> throw new IllegalArgumentException(
          "Unsupported destination denominator units: " + destinationUnits);
    };
  }

  /**
   * Infer a scaling factor without population information.
   *
   * <p>Infer the scale factor for converting between source and destination
   * units without population information. Uses a cached static map to avoid
   * repeated allocations.</p>
   *
   * @param source The source unit type
   * @param destination The destination unit type
   * @return The scale factor for conversion or null if not found
   */
  private BigDecimal inferScale(String source, String destination) {
    Map<String, BigDecimal> sourceScales = SCALE_MAP.get(source);
    if (sourceScales != null) {
      return sourceScales.get(destination);
    } else {
      return null;
    }
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
    if ("mt".equals(currentUnits) || "mteachyear".equals(currentUnits)) {
      return new EngineNumber(asVolume.getValue().multiply(KG_TO_MT_FACTOR), "kg");
    } else if ("kg".equals(currentUnits) || "kgeachyear".equals(currentUnits)) {
      return new EngineNumber(asVolume.getValue(), "kg");
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
    if ("kg".equals(currentUnits) || "kgeachyear".equals(currentUnits)) {
      return new EngineNumber(asVolume.getValue().multiply(MT_TO_KG_FACTOR), "mt");
    } else if ("mt".equals(currentUnits) || "mteachyear".equals(currentUnits)) {
      return new EngineNumber(asVolume.getValue(), "mt");
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

    boolean alreadyMt = "mt".equals(currentUnits) || "mteachyear".equals(currentUnits);
    boolean alreadyKg = "kg".equals(currentUnits)  || "kgeachyear".equals(currentUnits);

    if (alreadyMt || alreadyKg) {
      return target;
    } else if ("tCO2e".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      EngineNumber conversion = stateGetter.getSubstanceConsumption();
      String normalizedUnits = normalizeUnitString(conversion.getUnits());
      String[] conversionUnitPieces = splitUnits(normalizedUnits);
      String conversionNumeratorUnits = conversionUnitPieces[0];
      String newUnits = conversionUnitPieces[1];

      // Normalize conversion factor to match input units (recursive call to ensure unit consistency)
      EngineNumber conversionNumerator = new EngineNumber(conversion.getValue(), conversionNumeratorUnits);
      EngineNumber normalizedConversionNumerator = convert(conversionNumerator, currentUnits);
      BigDecimal normalizedConversionValue = normalizedConversionNumerator.getValue();

      BigDecimal newValue = originalValue.divide(normalizedConversionValue, MATH_CONTEXT);
      return new EngineNumber(newValue, newUnits);
    } else if ("kgCO2e".equals(currentUnits)) {
      // Convert kgCO2e to tCO2e first, then to volume
      BigDecimal kgco2eValue = target.getValue();
      BigDecimal tco2eValue = kgco2eValue.multiply(KGCO2E_TO_TCO2E_FACTOR);
      EngineNumber tco2eTarget = new EngineNumber(tco2eValue, "tCO2e");
      return toVolume(tco2eTarget);
    } else if ("unit".equals(currentUnits) || "units".equals(currentUnits) || "unitseachyear".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      EngineNumber conversion = stateGetter.getAmortizedUnitVolume();
      BigDecimal conversionValue = conversion.getValue();
      String normalizedUnits = normalizeUnitString(conversion.getUnits());
      String[] conversionUnitPieces = splitUnits(normalizedUnits);
      String newUnits = conversionUnitPieces[0];
      BigDecimal newValue = originalValue.multiply(conversionValue);
      return new EngineNumber(newValue, newUnits);
    } else if ("%".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      BigDecimal asRatio = originalValue.multiply(TO_PERCENT_FACTOR);
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

    boolean isUnitsAlias = "unit".equals(currentUnits) || "unitseachyear".equals(currentUnits);

    if ("units".equals(currentUnits)) {
      return target;
    } else if (isUnitsAlias) {
      return new EngineNumber(target.getValue(), "units");
    } else if ("kg".equals(currentUnits) || "mt".equals(currentUnits)) {
      EngineNumber conversion = stateGetter.getAmortizedUnitVolume();
      BigDecimal conversionValue = conversion.getValue();
      String normalizedUnits = normalizeUnitString(conversion.getUnits());
      String[] conversionUnitPieces = splitUnits(normalizedUnits);
      String expectedUnits = conversionUnitPieces[0];
      EngineNumber targetConverted = convert(target, expectedUnits);
      BigDecimal originalValue = targetConverted.getValue();
      BigDecimal newValue = originalValue.divide(conversionValue, MATH_CONTEXT);
      return new EngineNumber(newValue, "units");
    } else if ("tCO2e".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      EngineNumber conversion = stateGetter.getAmortizedUnitConsumption();
      String normalizedUnits = normalizeUnitString(conversion.getUnits());
      String[] conversionUnitPieces = splitUnits(normalizedUnits);
      String conversionNumeratorUnits = conversionUnitPieces[0];

      // Normalize conversion factor to match input units (recursive call to ensure unit consistency)
      EngineNumber conversionNumerator = new EngineNumber(conversion.getValue(), conversionNumeratorUnits);
      EngineNumber normalizedConversionNumerator = convert(conversionNumerator, currentUnits);
      BigDecimal normalizedConversionValue = normalizedConversionNumerator.getValue();

      BigDecimal newValue = originalValue.divide(normalizedConversionValue, MATH_CONTEXT);
      return new EngineNumber(newValue, "units");
    } else if ("kgCO2e".equals(currentUnits)) {
      // Convert kgCO2e to tCO2e first, then to units
      BigDecimal kgco2eValue = target.getValue();
      BigDecimal tco2eValue = kgco2eValue.multiply(KGCO2E_TO_TCO2E_FACTOR);
      EngineNumber tco2eTarget = new EngineNumber(tco2eValue, "tCO2e");
      return toUnits(tco2eTarget);
    } else if ("%".equals(currentUnits) || "%eachyear".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      BigDecimal asRatio = originalValue.multiply(TO_PERCENT_FACTOR);
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
  private EngineNumber toTonnesCo2eConsumption(EngineNumber target) {
    target = normalize(target);
    String currentUnits = target.getUnits();

    boolean alreadyCorrect = "tCO2e".equals(currentUnits) || "tCO2eeachyear".equals(currentUnits);

    boolean currentVolume = "kg".equals(currentUnits) || "mt".equals(currentUnits);
    boolean currentPop = "unit".equals(currentUnits) || "units".equals(currentUnits);
    boolean currentInfer = currentVolume || currentPop;

    if (alreadyCorrect) {
      return target;
    } else if ("kgCO2e".equals(currentUnits)) {
      // Convert kgCO2e to tCO2e
      BigDecimal kgco2eValue = target.getValue();
      BigDecimal tco2eValue = kgco2eValue.multiply(KGCO2E_TO_TCO2E_FACTOR);
      return new EngineNumber(tco2eValue, "tCO2e");
    } else if (currentInfer) {
      EngineNumber conversion = stateGetter.getSubstanceConsumption();
      String normalizedUnits = normalizeUnitString(conversion.getUnits());
      String[] conversionUnitPieces = splitUnits(normalizedUnits);
      String newUnits = conversionUnitPieces[0];
      String expectedUnits = conversionUnitPieces[1];

      if (isPerUnit(expectedUnits)) {
        return convertEmissionsPerUnit(target, conversion, newUnits, target.getUnits(), false);
      } else {
        return convertEmissionsPerVolume(target, conversion, newUnits, expectedUnits, false);
      }
    } else if ("%".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      BigDecimal asRatio = originalValue.multiply(TO_PERCENT_FACTOR);
      EngineNumber total = stateGetter.getGhgConsumption();
      BigDecimal newValue = total.getValue().multiply(asRatio);
      return new EngineNumber(newValue, "tCO2e");
    } else {
      throw new IllegalArgumentException("Unable to convert to consumption: " + currentUnits);
    }
  }

  /**
   * Convert a number to consumption as kgCO2e.
   *
   * @param target The EngineNumber to convert
   * @return Target converted to consumption as kgCO2e
   */
  private EngineNumber toKgCo2eConsumption(EngineNumber target) {
    target = normalize(target);
    String currentUnits = target.getUnits();

    boolean alreadyCorrect = "kgCO2e".equals(currentUnits) || "kgCO2eeachyear".equals(currentUnits);

    boolean currentVolume = "kg".equals(currentUnits) || "mt".equals(currentUnits);
    boolean currentPop = "unit".equals(currentUnits) || "units".equals(currentUnits);
    boolean currentInfer = currentVolume || currentPop;

    if (alreadyCorrect) {
      return target;
    } else if ("tCO2e".equals(currentUnits)) {
      // Convert tCO2e to kgCO2e
      BigDecimal tco2eValue = target.getValue();
      BigDecimal kgco2eValue = tco2eValue.multiply(TCO2E_TO_KGCO2E_FACTOR);
      return new EngineNumber(kgco2eValue, "kgCO2e");
    } else if (currentInfer) {
      EngineNumber conversion = stateGetter.getSubstanceConsumption();
      String normalizedUnits = normalizeUnitString(conversion.getUnits());
      String[] conversionUnitPieces = splitUnits(normalizedUnits);
      String newUnits = conversionUnitPieces[0];
      String expectedUnits = conversionUnitPieces[1];

      if (isPerUnit(expectedUnits)) {
        return convertEmissionsPerUnit(target, conversion, newUnits, target.getUnits(), true);
      } else {
        return convertEmissionsPerVolume(target, conversion, newUnits, expectedUnits, true);
      }
    } else if ("%".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      BigDecimal asRatio = originalValue.multiply(TO_PERCENT_FACTOR);
      EngineNumber total = stateGetter.getGhgConsumption();
      BigDecimal kgco2eTotal = total.getValue().multiply(TCO2E_TO_KGCO2E_FACTOR);
      BigDecimal newValue = kgco2eTotal.multiply(asRatio);
      return new EngineNumber(newValue, "kgCO2e");
    } else {
      throw new IllegalArgumentException("Unable to convert to kgCO2e consumption: " + currentUnits);
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

    boolean alreadyCorrect = "kwh".equals(currentUnits) || "kwheachyear".equals(currentUnits);

    if (alreadyCorrect) {
      return target;
    } else if (currentInfer) {
      EngineNumber conversion = stateGetter.getEnergyIntensity();
      BigDecimal conversionValue = conversion.getValue();
      String normalizedUnits = normalizeUnitString(conversion.getUnits());
      String[] conversionUnitPieces = splitUnits(normalizedUnits);
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
      BigDecimal asRatio = originalValue.multiply(TO_PERCENT_FACTOR);
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
    } else if (isYearsUnitStr(currentUnits)) {
      return new EngineNumber(target.getValue(), "years");
    } else if ("tCO2e".equals(currentUnits)) {
      BigDecimal perYearConsumptionValue = stateGetter.getGhgConsumption().getValue();
      BigDecimal newYears = target.getValue().divide(perYearConsumptionValue, MATH_CONTEXT);
      return new EngineNumber(newYears, "years");
    } else if ("kgCO2e".equals(currentUnits)) {
      // Convert kgCO2e to tCO2e first, then calculate years
      BigDecimal kgco2eValue = target.getValue();
      BigDecimal tco2eValue = kgco2eValue.multiply(KGCO2E_TO_TCO2E_FACTOR);
      BigDecimal perYearConsumptionValue = stateGetter.getGhgConsumption().getValue();
      BigDecimal newYears = tco2eValue.divide(perYearConsumptionValue, MATH_CONTEXT);
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
      BigDecimal perYearPopulation = stateGetter.getPopulationChange(this).getValue();
      BigDecimal newYears = target.getValue().divide(perYearPopulation, MATH_CONTEXT);
      return new EngineNumber(newYears, "years");
    } else if ("%".equals(currentUnits)) {
      BigDecimal originalValue = target.getValue();
      BigDecimal asRatio = originalValue.multiply(TO_PERCENT_FACTOR);
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

    if ("%".equals(currentUnits)) {
      return target;
    } else if (isYearsUnitStr(currentUnits)) {
      total = stateGetter.getYearsElapsed();
    } else if ("tCO2e".equals(currentUnits)) {
      total = stateGetter.getGhgConsumption();
    } else if ("kgCO2e".equals(currentUnits)) {
      // Convert kgCO2e to tCO2e first, then calculate percentage
      EngineNumber tco2eTotal = stateGetter.getGhgConsumption();
      BigDecimal kgco2eTotal = tco2eTotal.getValue().multiply(TCO2E_TO_KGCO2E_FACTOR);
      total = new EngineNumber(kgco2eTotal, "kgCO2e");
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
    String normalizedCurrentUnits = normalizeUnitString(currentUnits);

    boolean isPerUnit = normalizedCurrentUnits.endsWith("/unit") || normalizedCurrentUnits.endsWith("/units");

    if (!isPerUnit) {
      return target;
    } else {
      BigDecimal originalValue = target.getValue();
      String newUnits = normalizedCurrentUnits.split("/")[0];
      EngineNumber population = stateGetter.getPopulation();
      BigDecimal populationValue = population.getValue();
      BigDecimal newValue = originalValue.multiply(populationValue);

      return new EngineNumber(newValue, newUnits);
    }
  }

  /**
   * Convert a number where a units ratio has time in the denominator to a non-ratio units.
   *
   * @param target The value to normalize by time
   * @return Target without time in its units denominator
   */
  private EngineNumber normTime(EngineNumber target) {
    String currentUnits = target.getUnits();
    String normalizedCurrentUnits = normalizeUnitString(currentUnits);

    if (!getEndsWithPerYear(normalizedCurrentUnits)) {
      return target;
    } else {
      BigDecimal originalValue = target.getValue();
      String newUnits = normalizedCurrentUnits.split("/")[0];
      EngineNumber years = stateGetter.getYearsElapsed();
      BigDecimal yearsValue = years.getValue();
      BigDecimal newValue = originalValue.multiply(yearsValue);

      return new EngineNumber(newValue, newUnits);
    }
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
    String normalizedCurrentUnits = normalizeUnitString(currentUnits);

    boolean isCo2 = normalizedCurrentUnits.endsWith("/tCO2e");
    boolean isKgCo2 = normalizedCurrentUnits.endsWith("/kgCO2e");
    boolean isKwh = normalizedCurrentUnits.endsWith("/kwh");
    if (!isCo2 && !isKgCo2 && !isKwh) {
      return target;
    } else {
      EngineNumber targetConsumption;
      if (isCo2) {
        targetConsumption = stateGetter.getGhgConsumption();
      } else if (isKgCo2) {
        // Get tCO2e consumption and convert to kgCO2e
        EngineNumber tco2eConsumption = stateGetter.getGhgConsumption();
        BigDecimal kgco2eValue = tco2eConsumption.getValue().multiply(TCO2E_TO_KGCO2E_FACTOR);
        targetConsumption = new EngineNumber(kgco2eValue, "kgCO2e");
      } else {
        targetConsumption = stateGetter.getEnergyConsumption();
      }

      BigDecimal originalValue = target.getValue();
      String newUnits = normalizedCurrentUnits.split("/")[0];
      BigDecimal totalConsumptionValue = targetConsumption.getValue();
      BigDecimal newValue = originalValue.multiply(totalConsumptionValue);

      return new EngineNumber(newValue, newUnits);
    }
  }

  /**
   * Convert a number where a units ratio has volume in the denominator to a non-ratio units.
   *
   * @param target The value to normalize by volume
   * @return Target without volume in its units denominator
   */
  private EngineNumber normVolume(EngineNumber target) {
    String targetUnits = target.getUnits();
    String normalizedTargetUnits = normalizeUnitString(targetUnits);

    boolean divKg = normalizedTargetUnits.endsWith("/kg");
    boolean divMt = normalizedTargetUnits.endsWith("/mt");
    boolean needsNorm = divKg || divMt;
    if (!needsNorm) {
      return target;
    } else {
      String[] targetUnitPieces = splitUnits(normalizedTargetUnits);
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

  /**
   * Check if the expected units represent per-unit emissions factors.
   *
   * @param expectedUnits The expected units string from conversion factors
   * @return True if the units are per-unit (unit or units), false otherwise
   */
  private boolean isPerUnit(String expectedUnits) {
    return "unit".equals(expectedUnits) || "units".equals(expectedUnits);
  }

  /**
   * Handle conversion for per-unit emissions factors.
   *
   * <p>This method processes emissions conversion when the conversion factor
   * is expressed per equipment unit rather than per volume (kg/mt).</p>
   *
   * @param target The target EngineNumber to convert
   * @param conversion The conversion factor from state getter
   * @param newUnits The output units from the conversion factor
   * @param targetUnits The current units of the target value
   * @param isKgCo2eOutput Whether the final output should be in kgCO2e (true) or tCO2e (false)
   * @return The converted EngineNumber in the appropriate CO2e units
   */
  private EngineNumber convertEmissionsPerUnit(EngineNumber target, EngineNumber conversion,
      String newUnits, String targetUnits, boolean isKgCo2eOutput) {
    // For /unit factors, determine if target is volume or already population
    BigDecimal populationValue;

    if ("unit".equals(targetUnits) || "units".equals(targetUnits)) {
      // Target is already in population units
      populationValue = target.getValue();
    } else {
      // Target is in volume units, convert to population using amortized unit volume
      EngineNumber amortizedVolume = stateGetter.getAmortizedUnitVolume();
      BigDecimal volumePerUnit = amortizedVolume.getValue();
      BigDecimal targetVolume = target.getValue();
      populationValue = targetVolume.divide(volumePerUnit, MATH_CONTEXT);
    }

    BigDecimal conversionValue = conversion.getValue();

    if (isKgCo2eOutput) {
      // For kgCO2e output
      if ("kgCO2e".equals(newUnits)) {
        BigDecimal emissionsValue = populationValue.multiply(conversionValue);
        return new EngineNumber(emissionsValue, "kgCO2e");
      } else if ("tCO2e".equals(newUnits)) {
        BigDecimal tco2eValue = populationValue.multiply(conversionValue);
        BigDecimal kgco2eValue = tco2eValue.multiply(TCO2E_TO_KGCO2E_FACTOR);
        return new EngineNumber(kgco2eValue, "kgCO2e");
      } else {
        throw new IllegalArgumentException("Unsupported per-unit emissions type: " + newUnits);
      }
    } else {
      // For tCO2e output
      if ("tCO2e".equals(newUnits)) {
        BigDecimal emissionsValue = populationValue.multiply(conversionValue);
        return new EngineNumber(emissionsValue, "tCO2e");
      } else if ("kgCO2e".equals(newUnits)) {
        BigDecimal kgco2eValue = populationValue.multiply(conversionValue);
        BigDecimal tco2eValue = kgco2eValue.multiply(KGCO2E_TO_TCO2E_FACTOR);
        return new EngineNumber(tco2eValue, "tCO2e");
      } else {
        throw new IllegalArgumentException("Unsupported per-unit emissions type: " + newUnits);
      }
    }
  }

  /**
   * Handle conversion for per-volume emissions factors.
   *
   * <p>This method processes emissions conversion when the conversion factor
   * is expressed per volume unit (per kg or per mt).</p>
   *
   * @param target The target EngineNumber to convert
   * @param conversion The conversion factor from state getter
   * @param newUnits The output units from the conversion factor
   * @param expectedUnits The expected denominator units for conversion
   * @param isKgCo2eOutput Whether the final output should be in kgCO2e (true) or tCO2e (false)
   * @return The converted EngineNumber in the appropriate CO2e units
   */
  private EngineNumber convertEmissionsPerVolume(EngineNumber target, EngineNumber conversion,
      String newUnits, String expectedUnits, boolean isKgCo2eOutput) {
    EngineNumber targetConverted = convert(target, expectedUnits);
    BigDecimal originalValue = targetConverted.getValue();
    BigDecimal conversionValue = conversion.getValue();
    BigDecimal newValue = originalValue.multiply(conversionValue);

    if (!"tCO2e".equals(newUnits) && !"kgCO2e".equals(newUnits)) {
      throw new IllegalArgumentException("Unexpected units " + newUnits);
    }

    if (isKgCo2eOutput) {
      // Convert to kgCO2e if result is in tCO2e
      if ("tCO2e".equals(newUnits)) {
        newValue = newValue.multiply(TCO2E_TO_KGCO2E_FACTOR);
        newUnits = "kgCO2e";
      }
    } else {
      // Convert to tCO2e if result is in kgCO2e
      if ("kgCO2e".equals(newUnits)) {
        newValue = newValue.multiply(KGCO2E_TO_TCO2E_FACTOR);
        newUnits = "tCO2e";
      }
    }

    return new EngineNumber(newValue, newUnits);
  }
}
