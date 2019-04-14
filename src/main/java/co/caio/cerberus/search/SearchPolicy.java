package co.caio.cerberus.search;

import org.apache.lucene.search.Query;

public interface SearchPolicy {
  Query rewriteParsedFulltextQuery(Query query);

  boolean shouldComputeFacets(int totalHits);
}
