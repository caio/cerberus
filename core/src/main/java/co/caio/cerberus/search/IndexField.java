package co.caio.cerberus.search;

import co.caio.cerberus.model.Diet;
import co.caio.cerberus.model.DrillDown;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class IndexField {
  static final String RECIPE_ID = "recipeId";
  static final String CRAWL_URL = "crawlUrl";
  static final String NAME = "name";
  static final String INGREDIENTS = "ingredients";
  static final String NUM_INGREDIENTS = DrillDown.NUM_INGREDIENTS;
  static final String PREP_TIME = DrillDown.PREP_TIME;
  static final String COOK_TIME = DrillDown.COOK_TIME;
  static final String TOTAL_TIME = DrillDown.TOTAL_TIME;
  static final String CALORIES = "calories";
  static final String FAT_CONTENT = "fatContent";
  static final String PROTEIN_CONTENT = "proteinContent";
  static final String CARBOHYDRATE_CONTENT = "carbohydrateContent";
  static final String FULLTEXT = "fulltext";
  static final String FACET_DIET = "diet";
  static final String FACET_KEYWORD = "keyword";

  private static final Map<String, String> dietToFieldName =
      Collections.unmodifiableMap(
          Diet.knownDiets
              .stream()
              .collect(Collectors.toMap(Function.identity(), d -> String.format("diet_%s", d))));

  static String getFieldNameForDiet(String diet) {
    if (dietToFieldName.containsKey(diet)) {
      return dietToFieldName.get(diet);
    }
    throw new RuntimeException(String.format("Unknown diet `%s`", diet));
  }
}
