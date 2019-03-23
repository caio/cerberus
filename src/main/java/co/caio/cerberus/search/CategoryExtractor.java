package co.caio.cerberus.search;

import co.caio.cerberus.model.Recipe;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public interface CategoryExtractor {

  Map<String, Function<Recipe, Set<String>>> categoryToExtractor();

  Set<String> multiValuedCategories();

  CategoryExtractor NOOP = new CategoryExtractor.Builder().build();

  class Builder {
    private final Set<String> multiValuedCategories;
    private final Map<String, Function<Recipe, Set<String>>> categoryToExtractor;

    public Builder() {
      multiValuedCategories = new HashSet<>();
      categoryToExtractor = new HashMap<>();
    }

    void reset() {
      multiValuedCategories.clear();
      categoryToExtractor.clear();
    }

    public Builder addCategory(
        String name, boolean isMultiValued, Function<Recipe, Set<String>> labelExtractor) {
      if (categoryToExtractor.containsKey(name)) {
        throw new IllegalStateException("Can't have multiple categories named: " + name);
      }

      if (isMultiValued) {
        multiValuedCategories.add(name);
      }

      categoryToExtractor.put(name, labelExtractor);

      return this;
    }

    public CategoryExtractor build() {
      return new CategoryExtractorImpl(
          Collections.unmodifiableMap(categoryToExtractor),
          Collections.unmodifiableSet(multiValuedCategories));
    }

    private static class CategoryExtractorImpl implements CategoryExtractor {
      private final Map<String, Function<Recipe, Set<String>>> categoryToExtractor;
      private final Set<String> multiValuedCategories;

      CategoryExtractorImpl(
          Map<String, Function<Recipe, Set<String>>> categoryToExtractor,
          Set<String> multiValuedCategories) {
        this.categoryToExtractor = categoryToExtractor;
        this.multiValuedCategories = multiValuedCategories;
      }

      @Override
      public Map<String, Function<Recipe, Set<String>>> categoryToExtractor() {
        return categoryToExtractor;
      }

      @Override
      public Set<String> multiValuedCategories() {
        return multiValuedCategories;
      }
    }
  }
}
