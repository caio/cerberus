package co.caio.cerberus.search;

import static co.caio.cerberus.search.IndexField.*;

import co.caio.cerberus.model.SearchQuery;
import java.util.Optional;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class QueryInterpreter {
  private static final Logger logger = LoggerFactory.getLogger(QueryInterpreter.class);
  private final FulltextQueryParser queryParser;
  private final SearchPolicy searchPolicy;

  QueryInterpreter(IndexConfiguration conf, SearchPolicy policy) {
    queryParser = new FulltextQueryParser(conf.getAnalyzer());
    searchPolicy = policy;
  }

  Query toLuceneQuery(SearchQuery searchQuery) {
    var queryBuilder = new BooleanQuery.Builder();

    if (searchQuery.fulltext().isPresent()) {
      var parsedQuery = queryParser.parse(searchQuery.fulltext().get());
      if (searchPolicy != null) {
        searchPolicy.inspectParsedFulltextQuery(parsedQuery);
      }
      queryBuilder.add(parsedQuery, BooleanClause.Occur.MUST);
    }

    addFieldRangeQuery(queryBuilder, NUM_INGREDIENTS, searchQuery.numIngredients());
    addFieldRangeQuery(queryBuilder, COOK_TIME, searchQuery.cookTime());
    addFieldRangeQuery(queryBuilder, PREP_TIME, searchQuery.prepTime());
    addFieldRangeQuery(queryBuilder, TOTAL_TIME, searchQuery.totalTime());
    addFieldRangeQuery(queryBuilder, CALORIES, searchQuery.calories());
    addFieldRangeQuery(queryBuilder, FAT_CONTENT, searchQuery.fatContent());
    addFieldRangeQuery(queryBuilder, PROTEIN_CONTENT, searchQuery.proteinContent());
    addFieldRangeQuery(queryBuilder, CARBOHYDRATE_CONTENT, searchQuery.carbohydrateContent());

    searchQuery
        .dietThreshold()
        .forEach(
            (diet, score) ->
                queryBuilder.add(
                    FloatPoint.newRangeQuery(
                        IndexField.getFieldNameForDiet(diet), score, Float.MAX_VALUE),
                    BooleanClause.Occur.MUST));

    var luceneQuery = queryBuilder.build();
    logger.debug("Interpreted query {} as {}", searchQuery, luceneQuery);
    return luceneQuery;
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
  private static final Sort sortCalories = integerSorterWithDefault(CALORIES);

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
      case CALORIES:
        return sortCalories;
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
}
