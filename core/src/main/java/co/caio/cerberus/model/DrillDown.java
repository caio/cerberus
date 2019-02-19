package co.caio.cerberus.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DrillDown {
  public static final String NUM_INGREDIENTS = "numIngredients";
  public static final String PREP_TIME = "prepTime";
  public static final String COOK_TIME = "cookTime";
  public static final String TOTAL_TIME = "totalTime";

  private static final Map<String, Map<String, int[]>> fieldToRanges;

  static {
    var tmpFieldToRanges = new HashMap<String, Map<String, int[]>>();

    tmpFieldToRanges.put(
        NUM_INGREDIENTS,
        Map.of(
            "1-4", new int[] {1, 4},
            "5-10", new int[] {5, 10},
            "11+", new int[] {11, Integer.MAX_VALUE}));

    // Note that the ranges are interleaving and that's ok
    var timeRanges =
        Map.of(
            "0-15", new int[] {0, 15},
            "15-30", new int[] {15, 30},
            "30-60", new int[] {30, 60},
            "60+", new int[] {60, Integer.MAX_VALUE});

    tmpFieldToRanges.put(PREP_TIME, timeRanges);
    tmpFieldToRanges.put(COOK_TIME, timeRanges);
    tmpFieldToRanges.put(TOTAL_TIME, timeRanges);

    // Just saving me from myself
    tmpFieldToRanges.forEach(
        (f, labelToRanges) -> {
          for (var range : labelToRanges.values()) {
            assert range[0] < range[1];
          }
        });

    // XXX doesn't seem to make much sense to count ranges for nutrition data
    //     (calories, protein, etc)... or does it?
    fieldToRanges = Collections.unmodifiableMap(tmpFieldToRanges);
  }

  public static Map<String, Map<String, int[]>> getFieldToRanges() {
    return fieldToRanges;
  }

  public static boolean isValidRangeLabel(String field, String label) {
    if (fieldToRanges.containsKey(field)) {
      return fieldToRanges.get(field).containsKey(label);
    }
    return false;
  }

  public static int[] getRange(String field, String label) {
    if (fieldToRanges.containsKey(field)) {
      var rangeMap = fieldToRanges.get(field);
      if (rangeMap.containsKey(label)) {
        return rangeMap.get(label);
      }
    }
    throw new IllegalStateException(
        String.format("Invalid label `%s` for field `%s`", label, field));
  }
}
