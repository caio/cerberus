package co.caio.cerberus.search;

class IndexField {
  static final String RECIPE_ID = "recipeId";
  static final String NAME = "name";
  static final String INGREDIENTS = "ingredients";
  static final String INSTRUCTIONS = "instructions";
  static final String NUM_INGREDIENTS = "numIngredients";
  static final String PREP_TIME = "prepTime";
  static final String COOK_TIME = "cookTime";
  static final String TOTAL_TIME = "totalTIme";
  static final String CALORIES = "calories";
  static final String FAT_CONTENT = "fatContent";
  static final String PROTEIN_CONTENT = "proteinContent";
  static final String CARBOHYDRATE_CONTENT = "carbohydrateContent";
  static final String FULL_RECIPE = "fullRecipe";

  static String getFieldNameForDiet(String diet) {
    return "diet_" + diet;
  }
}
