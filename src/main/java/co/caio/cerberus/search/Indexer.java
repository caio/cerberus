package co.caio.cerberus.search;

import co.caio.cerberus.model.Recipe;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.OptionalInt;

public interface Indexer {
    void addRecipe(Recipe recipe) throws IOException;

    Directory getDirectory();

    int numDocs();

    void close() throws IOException;

    void commit() throws IOException;

    class Builder {
        private Directory directory;
        private Analyzer analyzer;
        private IndexWriterConfig writerConfig;
        private IndexWriterConfig.OpenMode openMode;

        public Builder reset() {
            directory = null;
            analyzer = null;
            writerConfig = null;
            openMode = null;
            return this;
        }

        private Builder directory(Directory dir) {
            directory = dir;
            return this;
        }

        public Builder directory(Path dir) {
            if (! dir.toFile().isDirectory()) {
                throw new IndexBuilderException(String.format("%s is not a directory", dir));
            }
            try {
                return directory(FSDirectory.open(dir));
            } catch (Exception e) {
                throw new IndexBuilderException(String.format("Exception opening %s: %s", dir, e));
            }
        }

        public Builder inMemory() {
            return directory(new RAMDirectory());
        }

        public Builder analyzer(Analyzer an) {
            analyzer = an;
            return this;
        }

        private Builder openMode(IndexWriterConfig.OpenMode mode) {
            openMode = mode;
            return this;
        }

        public Builder createMode() {
            return openMode(IndexWriterConfig.OpenMode.CREATE);
        }

        public Builder appendMode() {
            return openMode(IndexWriterConfig.OpenMode.APPEND);
        }

        public Builder createOrAppendMode() {
            return openMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }

        public Indexer build() {
            if (analyzer == null) {
                analyzer = new StandardAnalyzer();
            }

            if (writerConfig == null) {
                writerConfig = new IndexWriterConfig(analyzer);
            }

            if (openMode == null) {
                throw new IndexBuilderException("Missing `openMode`");
            }

            if (directory == null) {
                throw new IndexBuilderException("Missing `directory`");
            }

            writerConfig.setOpenMode(openMode);

            try {
                return new IndexerImpl(new IndexWriter(directory, writerConfig));
            } catch (IOException e) {
                throw new IndexBuilderException(String.format("Failure creating index writer: %s", e));
            }
        }

        private class IndexerImpl implements Indexer {
            private final IndexWriter indexWriter;

            private IndexerImpl(IndexWriter writer) {
                indexWriter = writer;
            }

            @Override
            public void addRecipe(Recipe recipe) throws IOException {
                var doc = new Document();
                doc.add(new LongPoint("recipeId", recipe.recipeId()));
                doc.add(new LongPoint("siteId", recipe.siteId()));
                doc.add(new StringField("crawlUrl", recipe.crawlUrl(), Field.Store.YES));

                doc.add(new TextField("name", recipe.name(), Field.Store.YES));
                doc.add(new TextField("instructions", recipe.instructions(), Field.Store.NO));
                doc.add(new TextField("description", recipe.description(), Field.Store.NO));

                doc.add(new TextField("ingredients",
                        String.join("\n", recipe.ingredients()), Field.Store.NO));
                doc.add(new IntPoint("numIngredients", recipe.ingredients().size()));

                // FIXME labels and keywords for faceting

                addOptionalIntField(doc, "prepTime", recipe.prepTime());
                addOptionalIntField(doc, "cookTime", recipe.cookTime());
                addOptionalIntField(doc, "totalTime", recipe.totalTime());

                addOptionalIntField(doc, "calories", recipe.calories());
                addOptionalIntField(doc, "carbohydrateContent", recipe.carbohydrateContent());
                addOptionalIntField(doc, "fatContent", recipe.fatContent());
                addOptionalIntField(doc, "proteinContent", recipe.proteinContent());
                indexWriter.addDocument(doc);
            }

            private void addOptionalIntField(Document doc, String fieldName, OptionalInt value) {
                if (value.isPresent()) {
                    doc.add(new IntPoint(fieldName, value.getAsInt()));
                }
            }

            @Override
            public Directory getDirectory() {
                return indexWriter.getDirectory();
            }

            @Override
            public int numDocs() {
                return indexWriter.numDocs();
            }

            @Override
            public void close() throws IOException {
                indexWriter.close();
            }

            @Override
            public void commit() throws IOException {
                indexWriter.commit();
            }

        }
    }

    class IndexBuilderException extends RuntimeException {
        IndexBuilderException(String message) {
            super(message);
        }
    }
}
