package co.caio.cerberus.search;

import co.caio.cerberus.lucene.FloatAssociationsThresholdCount;
import co.caio.cerberus.model.FacetData;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchResult;
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
    var tmp = new HashMap<String, LongRange[]>();

    tmp.put(
        IndexField.NUM_INGREDIENTS,
        new LongRange[] {
          new LongRange("1-4", 1, true, 4, true),
          new LongRange("5-10", 5, true, 10, true),
          new LongRange("10+", 1, true, Long.MAX_VALUE, true)
        });

    // Note that the ranges are interleaving and that's ok
    var timeRanges =
        new LongRange[] {
          new LongRange("0-15", 0, true, 15, true),
          new LongRange("15-30", 15, true, 30, true),
          new LongRange("30-60", 30, true, 60, true),
          new LongRange("60+", 60, true, Long.MAX_VALUE, true)
        };

    tmp.put(IndexField.PREP_TIME, timeRanges);
    tmp.put(IndexField.COOK_TIME, timeRanges);
    tmp.put(IndexField.TOTAL_TIME, timeRanges);

    // XXX doesn't seem to make much sense to count ranges for nutrition data
    //     (calories, protein, etc)... or does it?

    fieldToRanges = Collections.unmodifiableMap(tmp);
  }

  public SearchResult search(SearchQuery query) throws SearcherException {
    try {
      return _search(query);
    } catch (Exception wrapped) {
      throw new SearcherException(wrapped);
    }
  }

  public int numDocs() {
    return indexSearcher.getIndexReader().numDocs();
  }

  private class SearcherException extends Exception {
    private SearcherException(Exception e) {
      super(e);
    }
  }

  private Searcher(Searcher.Builder builder) {
    indexSearcher = new IndexSearcher(builder.indexReader);
    taxonomyReader = builder.taxonomyReader;
    facetsConfig = IndexConfiguration.getFacetsConfig();

    var moreLikeThis = new MoreLikeThis(builder.indexReader);
    moreLikeThis.setAnalyzer(IndexConfiguration.getAnalyzer());

    interpreter = new QueryInterpreter(moreLikeThis);
  }

  private SearchResult _search(SearchQuery query) throws Exception {
    var fc = new FacetsCollector();

    var result =
        FacetsCollector.search(
            indexSearcher,
            interpreter.toLuceneQuery(query),
            query.maxResults(),
            interpreter.toLuceneSort(query),
            fc);
    var builder = new SearchResult.Builder().totalHits(result.totalHits);

    for (int i = 0; i < result.scoreDocs.length; i++) {
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

    protected class SearcherBuilderException extends RuntimeException {
      SearcherBuilderException(String message) {
        super(message);
      }
    }
  }
}
