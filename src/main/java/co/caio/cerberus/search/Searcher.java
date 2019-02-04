package co.caio.cerberus.search;

import co.caio.cerberus.lucene.FloatAssociationsThresholdCount;
import co.caio.cerberus.model.DrillDown;
import co.caio.cerberus.model.FacetData;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.facet.range.LongRangeFacetCounts;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;

public class Searcher {
  private final IndexSearcher indexSearcher;
  private final TaxonomyReader taxonomyReader;
  private final QueryInterpreter interpreter;
  private final FacetsConfig facetsConfig;

  static final Map<String, LongRange[]> fieldToRanges;

  static {
    var tmpFieldToRanges = new HashMap<String, LongRange[]>();

    DrillDown.getFieldToRanges()
        .forEach(
            (field, labelToRanges) -> {
              var longRanges = new LongRange[labelToRanges.size()];
              var idx = 0;
              for (Entry<String, int[]> entry : labelToRanges.entrySet()) {
                var label = entry.getKey();
                var value = entry.getValue();
                longRanges[idx] = new LongRange(label, value[0], true, value[1], true);
                idx++;
              }
              tmpFieldToRanges.put(field, longRanges);
            });

    fieldToRanges = Collections.unmodifiableMap(tmpFieldToRanges);
  }

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

  public static class SearcherException extends RuntimeException {
    private SearcherException(Exception e) {
      super(e);
    }
  }

  private Searcher(Searcher.Builder builder) {
    // XXX create a worker pool to assign for the searcher maybe
    indexSearcher = new IndexSearcher(builder.indexReader);
    taxonomyReader = builder.taxonomyReader;
    facetsConfig = IndexConfiguration.getFacetsConfig();

    var moreLikeThis = new MoreLikeThis(builder.indexReader);
    moreLikeThis.setAnalyzer(IndexConfiguration.getAnalyzer());

    interpreter = new QueryInterpreter(moreLikeThis);
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
      builder.addRecipe(
          doc.getField(IndexField.RECIPE_ID).numericValue().longValue(),
          doc.get(IndexField.NAME),
          doc.get(IndexField.CRAWL_URL));
    }

    var maxFacets = query.maxFacets();
    if (maxFacets != 0) {
      var diets =
          new FloatAssociationsThresholdCount(
                  IndexField.FACET_DIET, query.dietThreshold(), taxonomyReader, facetsConfig, fc)
              .getTopChildren(maxFacets, IndexField.FACET_DIET);
      addFacetData(builder, diets);

      var keywords =
          new FastTaxonomyFacetCounts(IndexField.FACET_KEYWORD, taxonomyReader, facetsConfig, fc)
              .getTopChildren(maxFacets, IndexField.FACET_KEYWORD);
      addFacetData(builder, keywords);

      // XXX maybe extend LongRangeFacetCounts and make it reusable if garbage becomes a problem
      for (Entry<String, LongRange[]> entry : fieldToRanges.entrySet()) {
        var topK =
            new LongRangeFacetCounts(entry.getKey(), fc, entry.getValue())
                .getTopChildren(maxFacets, entry.getKey());
        addFacetData(builder, topK);
      }
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

    protected Builder indexReader(Directory dir) {
      try {
        indexReader = DirectoryReader.open(dir);
      } catch (Exception wrapped) {
        throw new SearcherBuilderException(wrapped.getMessage());
      }
      return this;
    }

    protected Builder taxonomyReader(Directory dir) {
      try {
        taxonomyReader = new DirectoryTaxonomyReader(dir);
      } catch (Exception wrapped) {
        throw new SearcherBuilderException(wrapped.getMessage());
      }
      return this;
    }

    public Searcher build() {
      if (indexReader == null) {
        throw new IllegalStateException("`indexReader` can't be null");
      }
      return new Searcher(this);
    }

    protected static class SearcherBuilderException extends RuntimeException {
      SearcherBuilderException(String message) {
        super(message);
      }
    }
  }
}
