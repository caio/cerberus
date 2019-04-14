package co.caio.cerberus.search;

import org.apache.lucene.search.Query;

class SearcherWithPolicy extends SearcherImpl implements Searcher {

  private final SearchPolicy searchPolicy;

  SearcherWithPolicy(Builder builder) {
    super(builder);
    searchPolicy = builder.getSearchPolicy();
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
}
