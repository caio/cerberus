package co.caio.cerberus.search;

import static co.caio.cerberus.search.IndexField.*;

import co.caio.cerberus.model.DrillDown;
import co.caio.cerberus.model.SearchQuery;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.queries.mlt.MoreLikeThis;
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
  private final Analyzer analyzer;
  private final FacetsConfig facetsConfig;
  private final MoreLikeThis moreLikeThis;
  private final FulltextQueryParser queryParser;

  QueryInterpreter(MoreLikeThis mlt) {
    analyzer = IndexConfiguration.DEFAULT_ANALYZER;
    facetsConfig = IndexConfiguration.getFacetsConfig();
    moreLikeThis = mlt;

    queryParser = new FulltextQueryParser(analyzer);
  }

  Query toLuceneQuery(SearchQuery searchQuery) throws IOException {
    var queryBuilder = new BooleanQuery.Builder();

    if (searchQuery.fulltext().isPresent()) {
      queryBuilder.add(queryParser.parse(searchQuery.fulltext().get()), BooleanClause.Occur.MUST);
    } else if (searchQuery.similarity().isPresent()) {
      queryBuilder.add(
          moreLikeThis.like(FULLTEXT, new StringReader(searchQuery.similarity().get())),
          BooleanClause.Occur.MUST);
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

    // no need to drill down on facets, we're done
    if (searchQuery.matchKeyword().isEmpty() && searchQuery.drillDown().isEmpty()) {
      return luceneQuery;
    }

    // TODO verify if matchKeyword really needs a drilldown (instead of filter)
    logger.debug(
        "Drilling it down with keyword={}, drillDown={}",
        searchQuery.matchKeyword(),
        searchQuery.drillDown());

    DrillDownQuery drillQuery;
    if (luceneQuery.clauses().isEmpty()) {
      drillQuery = new DrillDownQuery(facetsConfig);
    } else {
      drillQuery = new DrillDownQuery(facetsConfig, luceneQuery);
    }

    searchQuery.matchKeyword().forEach(kw -> drillQuery.add(IndexField.FACET_KEYWORD, kw));

    // TODO drill sideways
    searchQuery
        .drillDown()
        .forEach(
            dds -> {
              var range = DrillDown.getRange(dds.field(), dds.label());
              drillQuery.add(dds.field(), IntPoint.newRangeQuery(dds.field(), range[0], range[1]));
            });

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
}
