package co.caio.cerberus.search;

import co.caio.cerberus.model.Recipe;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;

public class Searcher {
    // FIXME make this private
    protected final IndexSearcher indexSearcher;

    private Searcher(Searcher.Builder builder) {
        indexSearcher = new IndexSearcher(builder.indexReader);
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
