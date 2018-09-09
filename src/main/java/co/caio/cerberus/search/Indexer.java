package co.caio.cerberus.search;

import co.caio.cerberus.model.Recipe;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;

import static co.caio.cerberus.search.IndexField.*;

public interface Indexer {
    void addRecipe(Recipe recipe) throws IOException;

    // TODO get rid of this
    Directory getDirectory();

    int numDocs();

    void close() throws IOException;

    void commit() throws IOException;

    class Builder {
        private Directory indexDirectory;
        private Directory taxonomyDirectory;
        private Analyzer analyzer;
        private IndexWriterConfig writerConfig;
        private IndexWriterConfig.OpenMode openMode;

        private static Logger logger = LoggerFactory.getLogger(Builder.class);
        // XXX This should be somewhere more accessible since the searcher also needs it
        private static String INDEX_DIR_NAME = "index";
        private static String TAXONOMY_DIR_NAME = "taxonomy";

        public Builder reset() {
            indexDirectory = null;
            taxonomyDirectory = null;
            analyzer = null;
            writerConfig = null;
            openMode = null;
            return this;
        }

        public Builder dataDirectory(Path dir) {
            indexDirectory = openDirectory(dir.resolve(INDEX_DIR_NAME));
            taxonomyDirectory = openDirectory(dir.resolve(TAXONOMY_DIR_NAME));
            return this;
        }

        public Builder inMemory() {
            indexDirectory = new RAMDirectory();
            taxonomyDirectory = new RAMDirectory();
            return this;
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

        private Directory openDirectory(Path dir) {
            if (! dir.toFile().exists() && dir.getParent().toFile().isDirectory()) {
                logger.debug("Creating directory %s", dir);
                try {
                    Files.createDirectory(dir);
                } catch (IOException wrapped) {
                    throw new IndexBuilderException(wrapped.getMessage());
                }
            }
            if (! dir.toFile().isDirectory()) {
                throw new IndexBuilderException(String.format("%s is not a directory", dir));
            }
            try {
                return FSDirectory.open(dir);
            } catch (Exception e) {
                throw new IndexBuilderException(String.format("Exception opening %s: %s", dir, e));
            }
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

            if (indexDirectory == null) {
                throw new IndexBuilderException("Missing `indexDirectory`");
            }

            if (taxonomyDirectory == null) {
                throw new IndexBuilderException("Missing `taxonomyDirectory`");
            }

            writerConfig.setOpenMode(openMode);

            try {
                return new IndexerImpl(
                        new IndexWriter(indexDirectory, writerConfig),
                        new DirectoryTaxonomyWriter(taxonomyDirectory, openMode));
            } catch (IOException e) {
                throw new IndexBuilderException(String.format("Failure creating index writer: %s", e));
            }
        }

        private class IndexerImpl implements Indexer {
            private final IndexWriter indexWriter;
            private final TaxonomyWriter taxonomyWriter;
            private final FacetsConfig facetsConfig;

            private IndexerImpl(IndexWriter writer, TaxonomyWriter taxWriter) {
                indexWriter = writer;
                taxonomyWriter = taxWriter;
                facetsConfig = new FacetsConfig();
            }

            @Override
            public void addRecipe(Recipe recipe) throws IOException {
                var doc = new Document();

                doc.add(new StoredField(RECIPE_ID, recipe.recipeId()));
                doc.add(new LongPoint(SITE_ID, recipe.siteId()));
                doc.add(new StringField(CRAWL_URL, recipe.crawlUrl(), Field.Store.YES));

                doc.add(new TextField(NAME, recipe.name(), Field.Store.YES));
                doc.add(new TextField(FULLTEXT, recipe.name(), Field.Store.NO));
                doc.add(new TextField(FULLTEXT, recipe.description(), Field.Store.NO));
                doc.add(new TextField(FULLTEXT, recipe.instructions(), Field.Store.NO));

                recipe.ingredients().forEach(i -> doc.add(new TextField(INGREDIENTS, i, Field.Store.NO)));
                doc.add(new IntPoint(NUM_INGREDIENTS, recipe.ingredients().size()));

                // FIXME labels and keywords for faceting

                addOptionalIntField(doc, PREP_TIME, recipe.prepTime());
                addOptionalIntField(doc, COOK_TIME, recipe.cookTime());
                addOptionalIntField(doc, TOTAL_TIME, recipe.totalTime());

                addOptionalIntField(doc, CALORIES, recipe.calories());
                addOptionalIntField(doc, CARBOHYDRATE_CONTENT, recipe.carbohydrateContent());
                addOptionalIntField(doc, FAT_CONTENT, recipe.fatContent());
                addOptionalIntField(doc, PROTEIN_CONTENT, recipe.proteinContent());

                indexWriter.addDocument(facetsConfig.build(taxonomyWriter, doc));
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
                taxonomyWriter.close();
            }

            @Override
            public void commit() throws IOException {
                indexWriter.commit();
                taxonomyWriter.commit();
            }

        }
    }

    class IndexBuilderException extends RuntimeException {
        IndexBuilderException(String message) {
            super(message);
        }
    }
}
