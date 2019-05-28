package co.caio.cerberus.search;

import co.caio.cerberus.model.Recipe;
import java.io.IOException;
import java.nio.file.Path;

public interface Indexer {
  void addRecipe(Recipe recipe) throws IOException;

  int numDocs();

  void close() throws IOException;

  void commit() throws IOException;

  void mergeSegments() throws IOException;

  class Factory {
    public static Indexer open(Path dir, CategoryExtractor extractor) {
      try {
        return new IndexerImpl(dir, extractor);
      } catch (Exception wrapped) {
        throw new IndexerException(wrapped);
      }
    }
  }

  class IndexerException extends RuntimeException {
    IndexerException(Exception e) {
      super(e);
    }
  }
}
