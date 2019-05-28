package co.caio.cerberus.search;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.lucene.search.Query;

class SearcherWithPolicy extends SearcherImpl implements Searcher {

  private final SearchPolicy searchPolicy;

  SearcherWithPolicy(Path dir, SearchPolicy policy) throws IOException {
    super(dir);
    searchPolicy = policy;
  }

  @Override
  boolean canComputeFacets(int totalHits) {
    return searchPolicy.shouldComputeFacets(totalHits);
  }

  @Override
  Query parseFulltext(String fulltext) {
    var parsed = super.parseFulltext(fulltext);
    return searchPolicy.rewriteParsedFulltextQuery(parsed);
  }

  @Override
  Query parseSimilarity(String similarText) {
    var parsed = super.parseSimilarity(similarText);
    return searchPolicy.rewriteParsedSimilarityQuery(parsed);
  }
}
