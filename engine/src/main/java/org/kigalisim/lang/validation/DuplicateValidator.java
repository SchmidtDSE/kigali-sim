/**
 * Utility class for validating unique names and preventing duplicates.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.kigalisim.lang.operation.EnableOperation;
import org.kigalisim.lang.operation.Operation;
import org.kigalisim.lang.program.ParsedSubstance;

/**
 * Utility class for validating unique names and preventing duplicates.
 *
 * <p>This class provides static methods for validating that collections of items
 * have unique names and for checking substance+equipment type combinations.</p>
 */
public class DuplicateValidator {

  /**
   * Validate that all items in a collection have unique names.
   *
   * @param <T> The type of items to validate
   * @param items The iterable collection of items to validate
   * @param nameExtractor Function to extract the name from each item
   * @param itemType The type of item being validated (for error messages)
   * @param context The context where validation is occurring (for error messages)
   * @return A map of validated items indexed by their unique names
   * @throws DuplicateValidationException if duplicate names are found
   */
  public static <T> Map<String, T> validateUniqueNames(
      Iterable<T> items,
      Function<T, String> nameExtractor,
      String itemType,
      String context) {
    
    Map<String, T> result = new HashMap<>();
    
    for (T item : items) {
      String name = nameExtractor.apply(item);
      if (result.containsKey(name)) {
        throw new DuplicateValidationException(itemType, name, context);
      }
      result.put(name, item);
    }
    
    return result;
  }

  /**
   * Validate that substance+equipment type combinations are unique within an application.
   *
   * @param substances The substances to validate for unique equipment type combinations
   * @param applicationName The name of the application (for error messages)
   * @throws DuplicateValidationException if duplicate substance+equipment combinations are found
   */
  public static void validateUniqueSubstanceEquipmentCombinations(
      Iterable<ParsedSubstance> substances,
      String applicationName) {
    
    // Track combinations of substance name + equipment type
    Set<String> seenCombinations = new HashSet<>();
    
    for (ParsedSubstance substance : substances) {
      String substanceName = substance.getName();
      Set<String> equipmentTypes = getEnabledEquipmentTypes(substance);
      
      for (String equipmentType : equipmentTypes) {
        String combination = substanceName + "+" + equipmentType;
        if (seenCombinations.contains(combination)) {
          String context = "application '" + applicationName + "'";
          String message = String.format(
              "Duplicate substance '%s' with equipment type '%s' found in %s. " 
              + "Each substance+equipment combination must be unique.",
              substanceName, equipmentType, context);
          throw new DuplicateValidationException("substance+equipment", combination, context, message);
        }
        seenCombinations.add(combination);
      }
    }
  }

  /**
   * Extract the equipment types (stream names) that are enabled for a substance.
   *
   * @param substance The substance to examine
   * @return Set of equipment type names that are enabled for this substance
   */
  private static Set<String> getEnabledEquipmentTypes(ParsedSubstance substance) {
    Set<String> equipmentTypes = new HashSet<>();
    
    for (Operation operation : substance.getOperations()) {
      if (operation instanceof EnableOperation) {
        EnableOperation enableOp = (EnableOperation) operation;
        String streamName = enableOp.getStream();
        equipmentTypes.add(streamName);
      }
    }
    
    return equipmentTypes;
  }
}