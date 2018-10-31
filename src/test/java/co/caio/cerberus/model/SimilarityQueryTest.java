package co.caio.cerberus.model;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.model.SimilarityQuery.Builder;
import org.junit.jupiter.api.Test;

class SimilarityQueryTest {
  @Test
  void validation() {
    assertThrows(IllegalStateException.class, () -> new Builder().build());
    assertThrows(IllegalStateException.class, () -> new Builder().fulltext("").build());
    assertThrows(IllegalStateException.class, () -> new Builder().fulltext(" ").build());
    assertThrows(
        IllegalStateException.class, () -> new Builder().fulltext("too few characters").build());

    var fulltext = "query with enough characters to pass the length restriction";
    assertThrows(
        IllegalStateException.class, () -> new Builder().fulltext(fulltext).maxResults(0).build());
    assertThrows(
        IllegalStateException.class,
        () -> new Builder().fulltext(fulltext).maxResults(200).build());
    assertDoesNotThrow(() -> new Builder().fulltext(fulltext).maxResults(10).build());
  }
}
