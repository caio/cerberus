package co.caio.cerberus.search;

class IndexField {
    // TODO wrap these into a String subclass if we ever
    //      have to leave the package scope
    static final String RECIPE_ID = "recipeId";
    static final String SITE_ID = "siteId";
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
    static final String FACET_DIM_DIET = "Diet";
    static final String FACET_DIET = "diet";
    static final String FACET_DIM_KEYWORD = "Keyword";
    static final String FACET_KEYWORD = "keyword";
}
