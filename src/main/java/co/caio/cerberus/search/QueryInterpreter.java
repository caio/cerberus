package co.caio.cerberus.search;

import co.caio.cerberus.model.SearchQuery;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.util.Optional;

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

        addFieldRangeQuery(queryBuilder, "numIngredients", searchQuery.numIngredients());
        addFieldRangeQuery(queryBuilder, "cookTime", searchQuery.cookTime());
        addFieldRangeQuery(queryBuilder, "prepTime", searchQuery.prepTime());
        addFieldRangeQuery(queryBuilder, "totalTime", searchQuery.totalTime());
        addFieldRangeQuery(queryBuilder, "calories", searchQuery.calories());
        addFieldRangeQuery(queryBuilder, "fatContent", searchQuery.fatContent());
        addFieldRangeQuery(queryBuilder, "proteinContent", searchQuery.proteinContent());
        addFieldRangeQuery(queryBuilder, "carbohydrateContent", searchQuery.carbohydrateContent());

        return queryBuilder.build();
    }

    private void addFieldRangeQuery(BooleanQuery.Builder builder, String fieldName, Optional<SearchQuery.RangedSpec> maybeRange) {
        if (maybeRange.isPresent()) {
            var range = maybeRange.get();
            builder.add(IntPoint.newRangeQuery(fieldName, range.start(), range.end()), BooleanClause.Occur.MUST);
        }
    }
}
