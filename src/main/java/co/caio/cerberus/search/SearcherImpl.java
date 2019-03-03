package co.caio.cerberus.search;

import co.caio.cerberus.lucene.FloatAssociationsThresholdCount;
import co.caio.cerberus.model.FacetData;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchResult;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.search.IndexSearcher;

public class SearcherImpl implements Searcher {
  private final IndexSearcher indexSearcher;
  private final TaxonomyReader taxonomyReader;
  private final QueryInterpreter interpreter;
  private final IndexConfiguration indexConfiguration;
  private final SearchPolicy searchPolicy;

  public SearchResult search(SearchQuery query) {
    try {
      return _search(query);
    } catch (IOException wrapped) {
      throw new SearcherException(wrapped);
    }
  }

  public int numDocs() {
    return indexSearcher.getIndexReader().numDocs();
  }

  SearcherImpl(Builder builder) {
    // XXX create a worker pool to assign for the searcher maybe
    indexSearcher = new IndexSearcher(builder.getIndexReader());
    taxonomyReader = builder.getTaxonomyReader();
    indexConfiguration = builder.getIndexConfiguration();
    searchPolicy = builder.getSearchPolicy();

    interpreter = new QueryInterpreter(indexConfiguration, searchPolicy);
  }

  private SearchResult _search(SearchQuery query) throws IOException {
    var fc = new FacetsCollector();

    var result =
        FacetsCollector.search(
            indexSearcher,
            interpreter.toLuceneQuery(query),
            query.offset() + query.maxResults(),
            interpreter.toLuceneSort(query),
            fc);
    var builder = new SearchResult.Builder().totalHits(result.totalHits);

    for (int i = query.offset(); i < result.scoreDocs.length; i++) {
      Document doc = indexSearcher.doc(result.scoreDocs[i].doc);
      builder.addRecipe(doc.getField(IndexField.RECIPE_ID).numericValue().longValue());
    }

    // TODO we should allow specifying which facets to collect
    var maxFacets = query.maxFacets();
    if (maxFacets != 0 && (searchPolicy == null || searchPolicy.shouldComputeFacets(result))) {
      var diets =
          new FloatAssociationsThresholdCount(
                  IndexField.FACET_DIET,
                  query.dietThreshold(),
                  taxonomyReader,
                  indexConfiguration.getFacetsConfig(),
                  fc)
              .getTopChildren(maxFacets, IndexField.FACET_DIET);
      addFacetData(builder, diets);
    }

    return builder.build();
  }

  private void addFacetData(SearchResult.Builder sb, FacetResult fr) {
    if (fr == null) {
      return;
    }
    var facetDataBuilder = new FacetData.Builder().dimension(fr.dim);
    for (int i = 0; i < fr.labelValues.length; i++) {
      facetDataBuilder.addChild(fr.labelValues[i].label, fr.labelValues[i].value.longValue());
    }
    sb.addFacets(facetDataBuilder.build());
  }
}
