package co.caio.cerberus.search;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

public class SearcherWithPolicy extends SearcherImpl implements Searcher {

  private final SearchPolicy searchPolicy;

  SearcherWithPolicy(Builder builder) {
    super(builder);
    searchPolicy = builder.getSearchPolicy();
  }

  @Override
  boolean canComputeFacets(TopDocs luceneResult) {
    return searchPolicy.shouldComputeFacets(luceneResult);
  }

  @Override
  Query parseFulltext(String fulltext) {
    var parsed = super.parseFulltext(fulltext);
    searchPolicy.inspectParsedFulltextQuery(parsed);
    return parsed;
  }
}
