package co.caio.cerberus.search;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class IndexField {
  // TODO wrap these into a String subclass if we ever
  //      have to leave the package scope
  static final String RECIPE_ID = "recipeId";
  static final String CRAWL_URL = "crawlUrl";
  static final String NAME = "name";
  static final String NUM_INGREDIENTS = "numIngredients";
  static final String INGREDIENTS = "ingredients";
  static final String PREP_TIME = "prepTime";
  static final String COOK_TIME = "cookTime";
  static final String TOTAL_TIME = "totalTime";
  static final String CALORIES = "calories";
  static final String FAT_CONTENT = "fatContent";
  static final String PROTEIN_CONTENT = "proteinContent";
  static final String CARBOHYDRATE_CONTENT = "carbohydrateContent";
  static final String FULLTEXT = "fulltext";
  static final String FACET_DIET = "diet";
  static final String FACET_KEYWORD = "keyword";

  private static final Map<String, String> dietToFieldName =
      Collections.unmodifiableMap(
          Stream.of("keto", "paleo", "lowcarb", "glutenfree", "vegan")
              .collect(Collectors.toMap(Function.identity(), x -> String.format("$diet_%s", x))));

  static String getFieldNameForDiet(String diet) {
    if (dietToFieldName.containsKey(diet)) {
      return dietToFieldName.get(diet);
    }
    throw new RuntimeException(String.format("Unknown diet `%s`", diet));
  }
}
