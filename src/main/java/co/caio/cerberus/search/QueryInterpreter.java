package co.caio.cerberus.search;

import co.caio.cerberus.model.SearchQuery;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.util.Optional;

import static co.caio.cerberus.search.IndexField.*;

public class QueryInterpreter {

    public QueryInterpreter() {
    }

    // TODO add tests
    public Query toLuceneQuery(SearchQuery searchQuery) {
        var queryBuilder = new BooleanQuery.Builder();

        if (searchQuery.fulltext().isPresent()) {
            // FIXME implement fulltext
            throw new RuntimeException("not implemented yet");
        }

        if (! searchQuery.withIngredients().isEmpty()) {
            // FIXME implement withIngredients
            throw new RuntimeException("not implemented yet");
        }

        if (! searchQuery.withoutIngredients().isEmpty()) {
            // FIXME implement withoutIngredients
            throw new RuntimeException("not implemented yet");
        }

        addFieldRangeQuery(queryBuilder, NUM_INGREDIENTS, searchQuery.numIngredients());
        addFieldRangeQuery(queryBuilder, COOK_TIME, searchQuery.cookTime());
        addFieldRangeQuery(queryBuilder, PREP_TIME, searchQuery.prepTime());
        addFieldRangeQuery(queryBuilder, TOTAL_TIME, searchQuery.totalTime());
        addFieldRangeQuery(queryBuilder, CALORIES, searchQuery.calories());
        addFieldRangeQuery(queryBuilder, FAT_CONTENT, searchQuery.fatContent());
        addFieldRangeQuery(queryBuilder, PROTEIN_CONTENT, searchQuery.proteinContent());
        addFieldRangeQuery(queryBuilder, CARBOHYDRATE_CONTENT, searchQuery.carbohydrateContent());

        return queryBuilder.build();
    }

    private void addFieldRangeQuery(BooleanQuery.Builder builder, String fieldName, Optional<SearchQuery.RangedSpec> maybeRange) {
        if (maybeRange.isPresent()) {
            var range = maybeRange.get();
            builder.add(IntPoint.newRangeQuery(fieldName, range.start(), range.end()), BooleanClause.Occur.MUST);
        }
    }
}
