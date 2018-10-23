package co.caio.cerberus.search;

import static co.caio.cerberus.search.IndexField.*;

import co.caio.cerberus.model.SearchQuery;
import java.io.IOException;
import java.util.Optional;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class QueryInterpreter {
  private final Analyzer analyzer;
  private final FacetsConfig facetsConfig;
  private static final Logger logger = LoggerFactory.getLogger(QueryInterpreter.class);

  QueryInterpreter() {
    analyzer = new StandardAnalyzer();
    facetsConfig = FacetConfiguration.getFacetsConfig();
  }

  Query toLuceneQuery(SearchQuery searchQuery) {
    var queryBuilder = new BooleanQuery.Builder();

    if (searchQuery.fulltext().isPresent()) {
      addTermQueries(
          queryBuilder, FULLTEXT, searchQuery.fulltext().get(), BooleanClause.Occur.MUST);
    }

    for (String ingredient : searchQuery.withIngredients()) {
      addTermQueries(queryBuilder, INGREDIENTS, ingredient, BooleanClause.Occur.MUST);
    }

    for (String notIngredient : searchQuery.withoutIngredients()) {
      addTermQueries(queryBuilder, INGREDIENTS, notIngredient, BooleanClause.Occur.MUST_NOT);
    }

    addFieldRangeQuery(queryBuilder, NUM_INGREDIENTS, searchQuery.numIngredients());
    addFieldRangeQuery(queryBuilder, COOK_TIME, searchQuery.cookTime());
    addFieldRangeQuery(queryBuilder, PREP_TIME, searchQuery.prepTime());
    addFieldRangeQuery(queryBuilder, TOTAL_TIME, searchQuery.totalTime());
    addFieldRangeQuery(queryBuilder, CALORIES, searchQuery.calories());
    addFieldRangeQuery(queryBuilder, FAT_CONTENT, searchQuery.fatContent());
    addFieldRangeQuery(queryBuilder, PROTEIN_CONTENT, searchQuery.proteinContent());
    addFieldRangeQuery(queryBuilder, CARBOHYDRATE_CONTENT, searchQuery.carbohydrateContent());

    // TODO actually allow specifying the threshold in the query
    searchQuery
        .matchDiet()
        .forEach(
            diet ->
                queryBuilder.add(
                    FloatPoint.newExactQuery(IndexField.getFieldNameForDiet(diet), 1f),
                    BooleanClause.Occur.MUST));

    var luceneQuery = queryBuilder.build();
    logger.debug("Interpreted query {} as {}", searchQuery, luceneQuery);

    // no need to drill down on facets, we're done
    if (searchQuery.matchKeyword().isEmpty()) {
      return luceneQuery;
    }

    logger.debug("Drilling it down with keyword={}", searchQuery.matchKeyword());

    DrillDownQuery drillQuery;
    if (luceneQuery.clauses().isEmpty()) {
      drillQuery = new DrillDownQuery(facetsConfig);
    } else {
      drillQuery = new DrillDownQuery(facetsConfig, luceneQuery);
    }

    searchQuery.matchKeyword().forEach(kw -> drillQuery.add(IndexField.FACET_KEYWORD, kw));
    return drillQuery;
  }

  private static Sort integerSorterWithDefault(String fieldName) {
    var field = new SortField(fieldName, Type.INT);
    field.setMissingValue(Integer.MAX_VALUE);
    return new Sort(field, SortField.FIELD_SCORE);
  }

  private static final Sort sortNumIngredients = integerSorterWithDefault(NUM_INGREDIENTS);
  private static final Sort sortPrepTime = integerSorterWithDefault(PREP_TIME);
  private static final Sort sortCookTime = integerSorterWithDefault(COOK_TIME);
  private static final Sort sortTotalTime = integerSorterWithDefault(TOTAL_TIME);

  Sort toLuceneSort(SearchQuery query) {
    switch (query.sort()) {
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
      default:
        throw new IllegalStateException(String.format("Unhandled sort order: %s", query.sort()));
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void addFieldRangeQuery(
      BooleanQuery.Builder builder, String fieldName, Optional<SearchQuery.RangedSpec> maybeRange) {
    if (maybeRange.isPresent()) {
      var range = maybeRange.get();
      builder.add(
          IntPoint.newRangeQuery(fieldName, range.start(), range.end()), BooleanClause.Occur.MUST);
    }
  }

  private void addTermQueries(
      BooleanQuery.Builder builder, String fieldName, String text, BooleanClause.Occur clause) {
    try {
      builder.add(textToTermQuery(fieldName, text), clause);
    } catch (IOException exception) {
      logger.error(
          "Building text query for input <{}> (Field {}) failed with {}",
          text,
          fieldName,
          exception);
    }
  }

  private Query textToTermQuery(String fieldName, String text) throws IOException {
    var builder = new BooleanQuery.Builder();

    try (var stream = analyzer.tokenStream(fieldName, text)) {
      var token = stream.addAttribute(CharTermAttribute.class);
      stream.reset();
      while (stream.incrementToken()) {
        builder.add(
            new TermQuery(new Term(fieldName, token.toString())), BooleanClause.Occur.SHOULD);
      }
      stream.end();
    }

    return builder.build();
  }
}
