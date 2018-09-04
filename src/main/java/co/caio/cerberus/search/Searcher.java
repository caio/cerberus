package co.caio.cerberus.search;

import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchResult;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;

public class Searcher {
    private final IndexSearcher indexSearcher;
    private final QueryInterpreter interpreter;
    private static final SearchResult emptyResults = new SearchResult.Builder().build();

    public SearchResult search(SearchQuery query, int maxResults) {
        try {
            return _search(query, maxResults);
        } catch (Exception ignored) {
            // XXX log me maybe
            return emptyResults;
        }
    }

    private Searcher(Searcher.Builder builder) {
        indexSearcher = new IndexSearcher(builder.indexReader);
        interpreter = new QueryInterpreter();
    }

    private SearchResult _search(SearchQuery query, int maxResults) throws IOException {
        var result = indexSearcher.search(interpreter.toLuceneQuery(query), maxResults);
        var builder = new SearchResult.Builder().totalHits(result.totalHits);
        for (int i = 0; i < result.scoreDocs.length; i++) {
            Document doc = indexSearcher.doc(result.scoreDocs[i].doc);
            builder.addRecipe(
                    doc.getField(IndexField.RECIPE_ID).numericValue().longValue(),
                    doc.get(IndexField.NAME),
                    doc.get(IndexField.CRAWL_URL));
        }
        return builder.build();
    }

    public static class Builder {
        private IndexReader indexReader;

        public Builder directory(Directory directory) {
            try {
                indexReader = DirectoryReader.open(directory);
            } catch (Exception e) {
                throw new SearcherBuilderException(String.format("Error creating index reader: %s", e));
            }
            return this;
        }

        public Builder directory(Path dir) {
            try (var d = FSDirectory.open(dir)){
                return directory(d);
            } catch (IOException e) {
                throw new SearcherBuilderException(String.format("Error opening directory: %s", e));
            }
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
