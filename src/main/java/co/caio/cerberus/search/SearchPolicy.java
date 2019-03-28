package co.caio.cerberus.search;

import org.apache.lucene.search.Query;

public interface SearchPolicy {
  void inspectParsedFulltextQuery(Query query);

  boolean shouldComputeFacets(int totalHits);
}
