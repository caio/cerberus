package co.caio.cerberus.search;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class SearcherImplTest {

  @Test
  void canFindEveryIndexedRecipe() {
    var searcher = (SearcherImpl) Util.getTestIndexer().buildSearcher();

    Util.getSampleRecipes()
        .forEach(
            sampleRecipe -> {
              try {
                var maybeDocId = searcher.findDocId(sampleRecipe.recipeId());
                assertTrue(maybeDocId.isPresent());
              } catch (IOException wrapped) {
                throw new RuntimeException(wrapped);
              }
            });
  }
}
