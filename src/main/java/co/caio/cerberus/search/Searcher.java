package co.caio.cerberus.search;

import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchResult;
import java.nio.file.Path;

public interface Searcher {

  SearchResult search(SearchQuery query);

  SearchResult findSimilar(String recipeText, int maxResults);

  int numDocs();

  class Factory {

    public static Searcher open(Path dir) {

      try {
        return new SearcherImpl(dir);
      } catch (Exception wrapped) {
        throw new SearcherException(wrapped);
      }
    }

    public static Searcher open(Path dir, SearchPolicy policy) {

      try {
        return new SearcherWithPolicy(dir, policy);
      } catch (Exception wrapped) {
        throw new SearcherException(wrapped);
      }
    }
  }

  class SearcherException extends RuntimeException {
    SearcherException(Exception e) {
      super(e);
    }
  }
}
