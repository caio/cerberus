package co.caio.cerberus.search;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.model.Recipe;
import co.caio.cerberus.model.SearchQuery;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CategoryExtractorTest {

  @Test
  void basicFunctionality(@TempDir Path dataDir) throws IOException {

    var ce =
        new CategoryExtractor.Builder()
            .addCategory(
                "num_ingredients",
                false,
                r -> {
                  var numIngredients = r.ingredients().size();
                  if (numIngredients <= 5) {
                    return Set.of("0,5");
                  } else if (numIngredients <= 10) {
                    return Set.of("6,10");
                  } else {
                    return Set.of("11+");
                  }
                })
            .addCategory(
                "calories",
                true,
                r -> {
                  Set<String> result = new HashSet<>();
                  var kcal = r.calories().orElse(Integer.MAX_VALUE);
                  if (kcal <= 200) {
                    result.add("0,200");
                  }
                  if (kcal <= 500) {
                    result.add("0,500");
                  }
                  return result;
                })
            .build();

    var indexer = Indexer.Factory.open(dataDir, ce);

    var categoryToWantedPerLabel =
        Map.of(
            "num_ingredients", Map.of("0,5", 1l, "6,10", 2l, "11+", 3l),
            "calories", Map.of("0,200", 2l, "0,500", 5l));
    // One for num_ingredients:"0,5"
    indexer.addRecipe(fakeRecipe(1, 2, 100));
    // Two for the "6,10"
    indexer.addRecipe(fakeRecipe(2, 6, 42));
    indexer.addRecipe(fakeRecipe(3, 7, 315));
    // And three for the "11+"
    indexer.addRecipe(fakeRecipe(4, 11, 710));
    indexer.addRecipe(fakeRecipe(5, 13, 420));
    indexer.addRecipe(fakeRecipe(6, 22, 500));

    indexer.commit();
    indexer.close();

    var searcher = Searcher.Factory.open(dataDir);
    var facets =
        searcher.search(new SearchQuery.Builder().fulltext("*").maxFacets(3).build()).facets();

    assertEquals(2, facets.size());

    facets
        .values()
        .forEach(
            fd -> {
              var wanted = categoryToWantedPerLabel.get(fd.dimension());
              assertEquals(wanted.size(), fd.children().size());

              fd.children()
                  .forEach(
                      (label, count) -> {
                        assertTrue(wanted.containsKey(label));
                        assertEquals(wanted.get(label), count);
                      });
            });
  }

  private Recipe fakeRecipe(long recipeId, int numIngredients, int calories) {
    return new Recipe.Builder()
        .recipeId(recipeId)
        .name("name")
        .crawlUrl("url")
        .slug("slug")
        .siteName("site")
        .addInstructions("there is nothing to do")
        .addAllIngredients(
            new Random()
                .ints(numIngredients)
                .mapToObj(Integer::toString)
                .collect(Collectors.toList()))
        .calories(calories)
        .build();
  }
}
