package co.caio.cerberus.search;

import static co.caio.cerberus.search.IndexField.*;

import co.caio.cerberus.model.Recipe;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

public interface Indexer {
  void addRecipe(Recipe recipe) throws IOException;

  int numDocs();

  void close() throws IOException;

  void commit() throws IOException;

  void mergeSegments() throws IOException;

  Searcher buildSearcher();

  class Builder {
    private Directory indexDirectory;
    private Directory taxonomyDirectory;
    private IndexConfiguration indexConfiguration;
    private IndexWriterConfig writerConfig;
    private IndexWriterConfig.OpenMode openMode;

    Builder reset() {
      indexDirectory = null;
      taxonomyDirectory = null;
      writerConfig = null;
      openMode = null;
      indexConfiguration = null;
      return this;
    }

    public Builder dataDirectory(Path dir) {
      if (!dir.toFile().isDirectory()) {
        throw new IndexBuilderException(String.format("'%s' is not a directory", dir));
      }

      indexConfiguration = new IndexConfiguration(dir);

      indexDirectory = indexConfiguration.openIndexDirectory();
      taxonomyDirectory = indexConfiguration.openTaxonomyDirectory();

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
      if (openMode == null) {
        throw new IndexBuilderException("Missing `openMode`");
      }

      if (indexDirectory == null || taxonomyDirectory == null || indexConfiguration == null) {
        throw new IndexBuilderException("dataDirectory() not set");
      }

      writerConfig = new IndexWriterConfig(indexConfiguration.getAnalyzer());
      writerConfig.setOpenMode(openMode);

      try {
        return new IndexerImpl(
            new IndexWriter(indexDirectory, writerConfig),
            new DirectoryTaxonomyWriter(taxonomyDirectory, openMode),
            indexConfiguration);
      } catch (IOException e) {
        throw new IndexBuilderException(String.format("Failure creating index writer: %s", e));
      }
    }

    private static final class IndexerImpl implements Indexer {
      private final IndexWriter indexWriter;
      private final DirectoryTaxonomyWriter taxonomyWriter;
      private final IndexConfiguration indexConfiguration;

      private IndexerImpl(
          IndexWriter writer, DirectoryTaxonomyWriter taxWriter, IndexConfiguration conf) {
        indexWriter = writer;
        taxonomyWriter = taxWriter;
        indexConfiguration = conf;
      }

      @Override
      public void addRecipe(Recipe recipe) throws IOException {
        var doc = new Document();

        doc.add(new StoredField(RECIPE_ID, recipe.recipeId()));

        doc.add(new TextField(NAME, recipe.name(), Store.NO));
        recipe.instructions().forEach(i -> doc.add(new TextField(INSTRUCTIONS, i, Store.NO)));
        recipe.ingredients().forEach(i -> doc.add(new TextField(INGREDIENTS, i, Store.NO)));

        recipe
            .diets()
            .forEach(
                (diet, score) -> {
                  if (score > 0) {
                    doc.add(new FloatPoint(IndexField.getFieldNameForDiet(diet), score));
                    if (score == 1) {
                      doc.add(new FacetField(FACET_DIET, diet));
                    }
                    // TODO per-diet thresholds, per-threshold counts
                  }
                });

        var numIngredients = recipe.ingredients().size();
        doc.add(new IntPoint(NUM_INGREDIENTS, numIngredients));
        doc.add(new NumericDocValuesField(NUM_INGREDIENTS, numIngredients));
        for (var range : RANGE_NUM_INGREDIENTS) {
          if (range.check(numIngredients)) {
            doc.add(new FacetField(FACET_NUM_INGREDIENTS, range.label));
          }
        }

        // Timing

        recipe
            .prepTime()
            .ifPresent(
                value -> {
                  doc.add(new IntPoint(PREP_TIME, value));
                  // For sorting
                  doc.add(new NumericDocValuesField(PREP_TIME, value));
                });

        recipe
            .cookTime()
            .ifPresent(
                value -> {
                  doc.add(new IntPoint(COOK_TIME, value));
                  // For sorting
                  doc.add(new NumericDocValuesField(COOK_TIME, value));
                });

        recipe
            .totalTime()
            .ifPresent(
                value -> {
                  doc.add(new IntPoint(TOTAL_TIME, value));
                  for (var range : RANGE_TOTAL_TIME) {
                    if (range.check(value)) {
                      doc.add(new FacetField(FACET_TOTAL_TIME, range.label));
                    }
                  }
                  // For sorting
                  doc.add(new NumericDocValuesField(TOTAL_TIME, value));
                });

        // Nutrition

        recipe
            .calories()
            .ifPresent(
                value -> {
                  doc.add(new IntPoint(CALORIES, value));
                  for (var range : RANGE_CALORIES) {
                    if (range.check(value)) {
                      doc.add(new FacetField(FACET_CALORIES, range.label));
                    }
                  }
                  // For sorting
                  doc.add(new NumericDocValuesField(CALORIES, value));
                });

        recipe
            .fatContent()
            .ifPresent(
                value -> {
                  doc.add(new FloatPoint(FAT_CONTENT, (float) value));
                  if (value <= 10) {
                    doc.add(new FacetField(FACET_FAT_CONTENT, "0,10"));
                  }
                });

        recipe
            .proteinContent()
            .ifPresent(value -> doc.add(new FloatPoint(PROTEIN_CONTENT, (float) value)));

        recipe
            .carbohydrateContent()
            .ifPresent(
                value -> {
                  doc.add(new FloatPoint(CARBOHYDRATE_CONTENT, (float) value));
                  if (value <= 30) {
                    doc.add(new FacetField(FACET_CARBOHYDRATE_CONTENT, "0,30"));
                  }
                });

        indexWriter.addDocument(indexConfiguration.getFacetsConfig().build(taxonomyWriter, doc));
      }

      private final LabeledRange[] RANGE_NUM_INGREDIENTS =
          new LabeledRange[] {
            new LabeledRange("0,5", 0, 5),
            new LabeledRange("6,10", 6, 10),
            new LabeledRange("11+", 11, Double.MAX_VALUE)
          };

      private final LabeledRange[] RANGE_TOTAL_TIME =
          new LabeledRange[] {
            new LabeledRange("0,15", 0, 15),
            new LabeledRange("15,30", 15, 30),
            new LabeledRange("30,60", 30, 60),
            new LabeledRange("60+", 60, Double.MAX_VALUE)
          };

      private final LabeledRange[] RANGE_CALORIES =
          new LabeledRange[] {new LabeledRange("0,200", 0, 200), new LabeledRange("0,500", 0, 500)};

      private class LabeledRange {
        final String label;
        final double start;
        final double end;

        LabeledRange(String label, double start, double end) {
          this.label = label;
          this.start = start;
          this.end = end;
        }

        boolean check(double value) {
          return value >= start && value <= end;
        }
      }

      @Override
      public int numDocs() {
        return indexWriter.getDocStats().numDocs;
      }

      @Override
      public void mergeSegments() throws IOException {
        indexWriter.forceMerge(1, true);
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

      @Override
      public Searcher buildSearcher() {
        return new Searcher.Builder()
            .indexConfiguration(indexConfiguration)
            .indexReader(indexWriter.getDirectory())
            .taxonomyReader(taxonomyWriter.getDirectory())
            .build();
      }
    }
  }

  class IndexBuilderException extends RuntimeException {
    IndexBuilderException(String message) {
      super(message);
    }
  }
}
