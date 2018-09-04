package co.caio.cerberus.search;

import co.caio.cerberus.model.SearchQuery;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

import static co.caio.cerberus.search.IndexField.*;

public class QueryInterpreter {
    private final Analyzer analyzer;
    private static final Logger logger = LoggerFactory.getLogger(QueryInterpreter.class);

    public QueryInterpreter() {
        analyzer = new StandardAnalyzer();
    }

    public Query toLuceneQuery(SearchQuery searchQuery) {
        var queryBuilder = new BooleanQuery.Builder();

        if (searchQuery.fulltext().isPresent()) {
            addTermQueries(queryBuilder, FULLTEXT, searchQuery.fulltext().get(), BooleanClause.Occur.MUST);
        }

        for (String ingredient: searchQuery.withIngredients()) {
            addTermQueries(queryBuilder, INGREDIENTS, ingredient, BooleanClause.Occur.MUST);
        }

        for (String notIngredient: searchQuery.withoutIngredients()) {
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

        var luceneQuery = queryBuilder.build();
        logger.debug("Interpreted query {} as {}", searchQuery, luceneQuery);
        return luceneQuery;
    }

    private void addFieldRangeQuery(BooleanQuery.Builder builder, String fieldName, Optional<SearchQuery.RangedSpec> maybeRange) {
        if (maybeRange.isPresent()) {
            var range = maybeRange.get();
            builder.add(IntPoint.newRangeQuery(fieldName, range.start(), range.end()), BooleanClause.Occur.MUST);
        }
    }

    private void addTermQueries(BooleanQuery.Builder builder, String fieldName, String text, BooleanClause.Occur clause) {
        try {
           builder.add(textToTermQuery(fieldName, text), clause);
        } catch (IOException exception) {
            logger.error("Building text query for input <{}> (Field {}) failed with {}",
                    text, fieldName, exception);
        }
    }

    private Query textToTermQuery(String fieldName, String text) throws IOException {
        var builder = new BooleanQuery.Builder();

        var stream = analyzer.tokenStream(fieldName, text);
        var token = stream.addAttribute(CharTermAttribute.class);

        try {
           stream.reset();
           while (stream.incrementToken()) {
               builder.add(new TermQuery(new Term(fieldName, token.toString())), BooleanClause.Occur.SHOULD);
           }
           stream.end();
        } finally {
            stream.close();
        }

        return builder.build();
    }
}
