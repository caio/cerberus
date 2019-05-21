package co.caio.cerberus.lucene;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FloatAssociationsThresholdCountTest {

  private static IndexSearcher indexSearcher;
  private static TaxonomyReader taxonomyReader;
  private static FacetsConfig config;

  @Test
  void checkCounts() throws Exception {
    var collector = new FacetsCollector();

    FacetsCollector.search(indexSearcher, new MatchAllDocsQuery(), 10, collector);

    // An empty map should behave as if every threshold is 1.0f
    // so we expect the counts to be 1 for every label
    // since we only have one doc with scores >= 1
    Map<String, Float> labelToThreshold = Map.of();
    var facets =
        new FloatAssociationsThresholdCount(
            "score", labelToThreshold, taxonomyReader, config, collector);
    var result = facets.getTopChildren(3, "score");
    for (LabelAndValue labelValue : result.labelValues) {
      assertEquals(1, labelValue.value.intValue());
    }

    // Now with every known path set to zero we expect all counts
    // to be 10 (the total number of docs) since all indexed
    // float values are >= 0
    labelToThreshold = Map.of("a", 0f, "b", 0f, "c", 0f);
    facets =
        new FloatAssociationsThresholdCount(
            "score", labelToThreshold, taxonomyReader, config, collector);
    result = facets.getTopChildren(3, "score");
    for (LabelAndValue labelValue : result.labelValues) {
      assertEquals(10, labelValue.value.intValue());
    }

    // Reduce the threshold for "a" to be 0.5 (so we expect 6 items)
    // and the threshold for "b" to 0.8 (expecting 3)
    // and not providing a threshold for "c", which defaults to 1 as the first test
    labelToThreshold = Map.of("a", 0.5f, "b", 0.8f);
    facets =
        new FloatAssociationsThresholdCount(
            "score", labelToThreshold, taxonomyReader, config, collector);
    result = facets.getTopChildren(3, "score");
    for (LabelAndValue labelValue : result.labelValues) {
      switch (labelValue.label) {
        case "a":
          assertEquals(6, labelValue.value.intValue());
          break;
        case "b":
          assertEquals(3, labelValue.value.intValue());
          break;
        case "c":
          assertEquals(1, labelValue.value.intValue());
          break;
        default:
          throw new IllegalStateException("This should never happen!");
      }
    }
  }

  @BeforeAll
  static void setUp(@TempDir Path tmpDir) throws IOException {
    final var indexDir = FSDirectory.open(tmpDir.resolve("index"));
    final var taxoDir = FSDirectory.open(tmpDir.resolve("taxo"));

    config = new FacetsConfig();
    config.setMultiValued("score", true);
    config.setIndexFieldName("score", "score");

    var iwc = new IndexWriterConfig();
    iwc.setOpenMode(OpenMode.CREATE);
    var iw = new IndexWriter(indexDir, iwc);
    var tw = new DirectoryTaxonomyWriter(taxoDir);

    // Create 10 documents looking like:
    //   { "score": { "a": 0.1, "b": 0.1, "c": 0.1 }
    //   { "score": { "a": 0.2, "b": 0.2, "c": 0.2 }
    //   ...
    //   { "score": { "a": 1.0, "b": 1.0, "c": 1.0 }
    for (float i = 1; i <= 10; i++) {
      var doc = new Document();
      doc.add(new FloatThresholdField(i / 10, "score", "a"));
      doc.add(new FloatThresholdField(i / 10, "score", "b"));
      doc.add(new FloatThresholdField(i / 10, "score", "c"));
      iw.addDocument(config.build(tw, doc));
    }

    iw.close();
    tw.close();

    var indexReader = DirectoryReader.open(indexDir);
    indexSearcher = new IndexSearcher(indexReader);
    taxonomyReader = new DirectoryTaxonomyReader(taxoDir);
  }
}
