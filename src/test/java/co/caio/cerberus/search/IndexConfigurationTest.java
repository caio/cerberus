package co.caio.cerberus.search;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.search.IndexConfiguration.IndexConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.lucene.facet.FacetsConfig;
import org.junit.jupiter.api.Test;

class IndexConfigurationTest {

  @Test
  void canCreateEmpty() {
    assertDoesNotThrow(
        () -> new IndexConfiguration(Files.createTempDirectory("indexconfig-"), Set.of()));
  }

  @Test
  void multiValuedDimensionsConfiguresFacetConfig() throws IOException {
    var originalMv = Set.of("a", "b", "c");
    var config = new IndexConfiguration(Files.createTempDirectory("indexconfig-"), originalMv);

    var configuredMv = extractMultiValued(config.getFacetsConfig());

    assertEquals(originalMv, configuredMv);
  }

  @Test
  void cantLoadFromConfigIfItDoesNotExist() {
    assertThrows(
        IndexConfigurationException.class,
        () -> IndexConfiguration.fromBaseDirectory(Files.createTempDirectory("indexconfig-")));
  }

  @Test
  void loadFromConfigWorks() throws IOException {
    var base = Files.createTempDirectory("indexconfig-");
    var multiValued = Set.of("a", "b", "c", "d");
    var config = new IndexConfiguration(base, multiValued);

    config.save();
    assertTrue(base.resolve(IndexConfiguration.CONFIG_NAME).toFile().exists());

    var loaded = IndexConfiguration.fromBaseDirectory(base);
    assertEquals(multiValued, extractMultiValued(loaded.getFacetsConfig()));
  }

  private Set<String> extractMultiValued(FacetsConfig fc) {
    return fc.getDimConfigs()
        .entrySet()
        .stream()
        .filter(e -> e.getValue().multiValued)
        .map(Entry::getKey)
        .collect(Collectors.toSet());
  }
}
