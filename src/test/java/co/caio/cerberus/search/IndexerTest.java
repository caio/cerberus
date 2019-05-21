package co.caio.cerberus.search;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IndexerTest {
  @Test
  void badUsage() {
    var exc = Indexer.IndexBuilderException.class;
    var builder = new Indexer.Builder();
    assertThrows(exc, builder::build);
    assertThrows(exc, () -> builder.reset().createMode().build());
    assertThrows(
        exc,
        () -> builder.reset().dataDirectory(Path.of("/this/doesnt/exist")).createMode().build());
  }

  @Test
  void simpleLocalIndexer(@TempDir Path tempDir) throws IOException {
    var index = new Indexer.Builder().dataDirectory(tempDir).createMode().build();
    assertEquals(0, index.numDocs());
    index.addRecipe(Util.getBasicRecipe());
    assertEquals(1, index.numDocs());
    index.close();

    // Reopening it should still allow us to read its documents
    var newIndexSameDir = new Indexer.Builder().dataDirectory(tempDir).appendMode().build();
    assertEquals(1, newIndexSameDir.numDocs());
    newIndexSameDir.close();

    // But opening should erase the old data
    var destructiveIndex = new Indexer.Builder().dataDirectory(tempDir).createMode().build();
    assertEquals(0, destructiveIndex.numDocs());
  }
}
