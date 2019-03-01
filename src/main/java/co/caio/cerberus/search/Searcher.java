package co.caio.cerberus.search;

import co.caio.cerberus.lucene.FloatAssociationsThresholdCount;
import co.caio.cerberus.model.FacetData;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchResult;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;

public class Searcher {
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

  static class SearcherException extends RuntimeException {
    private SearcherException(Exception e) {
      super(e);
    }
  }

  private Searcher(Searcher.Builder builder) {
    // XXX create a worker pool to assign for the searcher maybe
    indexSearcher = new IndexSearcher(builder.indexReader);
    taxonomyReader = builder.taxonomyReader;
    indexConfiguration = builder.indexConfiguration;
    searchPolicy = builder.searchPolicy;

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

  public static class Builder {
    private IndexReader indexReader;
    private TaxonomyReader taxonomyReader;
    private IndexConfiguration indexConfiguration;
    private SearchPolicy searchPolicy;

    public Builder dataDirectory(Path dir) {
      try {
        var indexDirectory = FileSystem.openDirectory(dir.resolve(FileSystem.INDEX_DIR_NAME));
        var taxonomyDirectory = FileSystem.openDirectory(dir.resolve(FileSystem.TAXONOMY_DIR_NAME));
        indexReader = DirectoryReader.open(indexDirectory);
        taxonomyReader = new DirectoryTaxonomyReader(taxonomyDirectory);
      } catch (Exception e) {
        throw new SearcherBuilderException(e.getMessage());
      }
      return this;
    }

    Builder indexReader(Directory dir) {
      try {
        indexReader = DirectoryReader.open(dir);
      } catch (Exception wrapped) {
        throw new SearcherBuilderException(wrapped.getMessage());
      }
      return this;
    }

    Builder indexConfiguration(IndexConfiguration conf) {
      indexConfiguration = conf;
      return this;
    }

    Builder taxonomyReader(Directory dir) {
      try {
        taxonomyReader = new DirectoryTaxonomyReader(dir);
      } catch (Exception wrapped) {
        throw new SearcherBuilderException(wrapped.getMessage());
      }
      return this;
    }

    public Builder searchPolicy(SearchPolicy policy) {
      searchPolicy = policy;
      return this;
    }

    public Searcher build() {
      if (indexReader == null) {
        throw new IllegalStateException("`indexReader` can't be null");
      }
      if (indexConfiguration == null) {
        indexConfiguration = new IndexConfiguration();
      }
      return new Searcher(this);
    }

    static class SearcherBuilderException extends RuntimeException {
      SearcherBuilderException(String message) {
        super(message);
      }
    }
  }
}
