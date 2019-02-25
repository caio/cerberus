package co.caio.cerberus.search;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

public interface SearchPolicy {
  void inspectParsedFulltextQuery(Query query);

  boolean shouldComputeFacets(TopDocs result);
}
