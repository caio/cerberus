package co.caio.cerberus.search;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IndexerTest {

  @Test
  void simpleLocalIndexer(@TempDir Path tempDir) throws IOException {
    var index = Indexer.Factory.open(tempDir, CategoryExtractor.NOOP);
    assertEquals(0, index.numDocs());
    index.addRecipe(Util.getBasicRecipe());
    assertEquals(1, index.numDocs());
    index.close();

    // Reopening it should still allow us to read its documents
    var newIndexSameDir = Indexer.Factory.open(tempDir, CategoryExtractor.NOOP);
    assertEquals(1, newIndexSameDir.numDocs());
    newIndexSameDir.close();
  }
}
