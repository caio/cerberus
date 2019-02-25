package co.caio.cerberus.search;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

interface SearchPolicy {
  void inspectLuceneQuery(Query query);

  boolean shouldComputeFacets(TopDocs result);
}
