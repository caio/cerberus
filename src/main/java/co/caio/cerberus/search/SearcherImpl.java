package co.caio.cerberus.search;

import static co.caio.cerberus.search.IndexField.*;

import co.caio.cerberus.model.FacetData;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchQuery.SortOrder;
import co.caio.cerberus.model.SearchResult;
import java.io.IOException;
import java.io.StringReader;
import java.util.OptionalInt;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TopDocs;

class SearcherImpl implements Searcher {

  private static final Sort sortNumIngredients = integerSorterWithDefault(NUM_INGREDIENTS);
  private static final Sort sortPrepTime = integerSorterWithDefault(PREP_TIME);
  private static final Sort sortCookTime = integerSorterWithDefault(COOK_TIME);
  private static final Sort sortTotalTime = integerSorterWithDefault(TOTAL_TIME);
  private static final Sort sortCalories = integerSorterWithDefault(CALORIES);

  private static final SearchResult EMPTY_SEARCH_RESULT =
      new SearchResult.Builder().totalHits(0).build();

  private final IndexSearcher indexSearcher;
  private final TaxonomyReader taxonomyReader;
  private final IndexConfiguration indexConfiguration;
  private final FulltextQueryParser queryParser;
  private final MoreLikeThis moreLikeThis;

  SearcherImpl(Builder builder) {
    indexSearcher = new IndexSearcher(builder.getIndexReader());
    taxonomyReader = builder.getTaxonomyReader();
    indexConfiguration = builder.getIndexConfiguration();
    queryParser = new FulltextQueryParser(indexConfiguration.getAnalyzer());

    moreLikeThis = new MoreLikeThis(builder.getIndexReader());
    moreLikeThis.setAnalyzer(indexConfiguration.getAnalyzer());
    // Ignore words that occurr in more than 50% of recipes
    moreLikeThis.setMaxDocFreqPct(50);
    // Relevant for docId-based similarity
    moreLikeThis.setFieldNames(new String[] {FULL_RECIPE});
  }

  private static Sort integerSorterWithDefault(String fieldName) {
    var field = new SortField(fieldName, Type.INT);
    field.setMissingValue(Integer.MAX_VALUE);
    return new Sort(field, SortField.FIELD_SCORE);
  }

  public SearchResult search(SearchQuery query) {
    try {
      return _search(query);
    } catch (IOException wrapped) {
      throw new SearcherException(wrapped);
    }
  }

  @Override
  public SearchResult findSimilar(String recipeText, int maxResults) {
    try {
      var query = moreLikeThis.like(FULL_RECIPE, new StringReader(recipeText));
      var result = indexSearcher.search(query, maxResults);

      var builder = new SearchResult.Builder().totalHits(result.totalHits.value);

      for (int i = 0; i < result.scoreDocs.length; i++) {
        Document doc = indexSearcher.doc(result.scoreDocs[i].doc);
        builder.addRecipe(doc.getField(RECIPE_ID).numericValue().longValue());
      }

      return builder.build();
    } catch (IOException wrapped) {
      throw new SearcherException(wrapped);
    }
  }

  @Override
  public SearchResult findSimilar(long recipeId, int maxResults) {
    try {
      var docId = findDocId(recipeId);

      if (docId.isEmpty()) {
        return EMPTY_SEARCH_RESULT;
      }

      // We use `maxResults + 1` because we'll filter out the
      // given recipeId from the results
      var result = indexSearcher.search(moreLikeThis.like(docId.getAsInt()), maxResults + 1);

      var builder = new SearchResult.Builder();

      int totalHits = 0;
      for (int i = 0; i < result.scoreDocs.length && totalHits <= maxResults; i++) {
        Document doc = indexSearcher.doc(result.scoreDocs[i].doc);
        long foundRecipeId = doc.getField(RECIPE_ID).numericValue().longValue();

        if (foundRecipeId != recipeId) {
          builder.addRecipe(foundRecipeId);
          totalHits++;
        }
      }

      return builder.totalHits(totalHits).build();
    } catch (IOException wrapped) {
      throw new SearcherException(wrapped);
    }
  }

  OptionalInt findDocId(long recipeId) throws IOException {
    var result = indexSearcher.search(LongPoint.newExactQuery(RECIPE_ID, recipeId), 1);

    if (result.scoreDocs.length == 0) {
      return OptionalInt.empty();
    }

    return OptionalInt.of(result.scoreDocs[0].doc);
  }

  public int numDocs() {
    return indexSearcher.getIndexReader().numDocs();
  }

  private SearchResult _search(SearchQuery query) throws IOException {
    final int maxFacets = query.maxFacets();

    var luceneQuery = toLuceneQuery(query);

    final int count = indexSearcher.count(luceneQuery);
    final boolean computeFacets = maxFacets > 0 && canComputeFacets(count);

    var builder = new SearchResult.Builder().totalHits(count);
    TopDocs result;

    if (computeFacets) {
      var fc = new FacetsCollector();

      result =
          FacetsCollector.search(
              indexSearcher,
              luceneQuery,
              query.offset() + query.maxResults(),
              toLuceneSort(query.sort()),
              fc);

      var staticFacets =
          new FastTaxonomyFacetCounts(taxonomyReader, indexConfiguration.getFacetsConfig(), fc);

      staticFacets.getAllDims(maxFacets).forEach(fr -> addFacetData(builder, fr));

    } else {
      result =
          indexSearcher.search(
              luceneQuery, query.offset() + query.maxResults(), toLuceneSort(query.sort()));
    }

    for (int i = query.offset(); i < result.scoreDocs.length; i++) {
      Document doc = indexSearcher.doc(result.scoreDocs[i].doc);
      builder.addRecipe(doc.getField(RECIPE_ID).numericValue().longValue());
    }

    return builder.build();
  }

  boolean canComputeFacets(int unused) {
    return true;
  }

  private void addFacetData(SearchResult.Builder sb, FacetResult fr) {
    if (fr == null) {
      return;
    }

    var facetDataBuilder = new FacetData.Builder().dimension(fr.dim);
    for (int i = 0; i < fr.labelValues.length; i++) {
      facetDataBuilder.putChildren(fr.labelValues[i].label, fr.labelValues[i].value.longValue());
    }
    sb.putFacets(fr.dim, facetDataBuilder.build());
  }

  Query parseFulltext(String fulltext) {
    return queryParser.parse(fulltext);
  }

  Sort toLuceneSort(SortOrder sortOrder) {
    switch (sortOrder) {
      case RELEVANCE:
        return Sort.RELEVANCE;
      case NUM_INGREDIENTS:
        return sortNumIngredients;
      case PREP_TIME:
        return sortPrepTime;
      case COOK_TIME:
        return sortCookTime;
      case TOTAL_TIME:
        return sortTotalTime;
      case CALORIES:
        return sortCalories;
      default:
        throw new IllegalStateException(String.format("Unhandled sort order: %s", sortOrder));
    }
  }

  Query toLuceneQuery(SearchQuery searchQuery) {
    var queryBuilder = new BooleanQuery.Builder();

    searchQuery
        .fulltext()
        .ifPresent(fulltext -> queryBuilder.add(parseFulltext(fulltext), Occur.MUST));

    searchQuery
        .numIngredients()
        .ifPresent(
            range ->
                queryBuilder.add(
                    IntPoint.newRangeQuery(NUM_INGREDIENTS, range.start(), range.end()),
                    Occur.MUST));

    searchQuery
        .cookTime()
        .ifPresent(
            range ->
                queryBuilder.add(
                    IntPoint.newRangeQuery(COOK_TIME, range.start(), range.end()), Occur.MUST));

    searchQuery
        .prepTime()
        .ifPresent(
            range ->
                queryBuilder.add(
                    IntPoint.newRangeQuery(PREP_TIME, range.start(), range.end()), Occur.MUST));

    searchQuery
        .totalTime()
        .ifPresent(
            range ->
                queryBuilder.add(
                    IntPoint.newRangeQuery(TOTAL_TIME, range.start(), range.end()), Occur.MUST));

    searchQuery
        .calories()
        .ifPresent(
            range ->
                queryBuilder.add(
                    IntPoint.newRangeQuery(CALORIES, range.start(), range.end()), Occur.MUST));

    searchQuery
        .fatContent()
        .ifPresent(
            range ->
                queryBuilder.add(
                    FloatPoint.newRangeQuery(FAT_CONTENT, range.start(), range.end()), Occur.MUST));

    searchQuery
        .proteinContent()
        .ifPresent(
            range ->
                queryBuilder.add(
                    FloatPoint.newRangeQuery(PROTEIN_CONTENT, range.start(), range.end()),
                    Occur.MUST));

    searchQuery
        .carbohydrateContent()
        .ifPresent(
            range ->
                queryBuilder.add(
                    FloatPoint.newRangeQuery(CARBOHYDRATE_CONTENT, range.start(), range.end()),
                    Occur.MUST));

    searchQuery
        .dietThreshold()
        .forEach(
            (diet, score) ->
                queryBuilder.add(
                    FloatPoint.newRangeQuery(
                        IndexField.getFieldNameForDiet(diet), score, Float.MAX_VALUE),
                    Occur.MUST));

    return queryBuilder.build();
  }
}
