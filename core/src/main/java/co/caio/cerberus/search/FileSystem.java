package co.caio.cerberus.search;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystem {
  private static final Logger logger = LoggerFactory.getLogger(FileSystem.class);

  static final String INDEX_DIR_NAME = "index";
  static final String TAXONOMY_DIR_NAME = "taxonomy";

  static Directory openDirectory(Path dir) throws Exception {
    return openDirectory(dir, false);
  }

  static Directory openDirectory(Path dir, boolean create) throws Exception {
    if (create && !dir.toFile().exists() && dir.getParent().toFile().isDirectory()) {
      logger.debug("Creating directory {}", dir);
      Files.createDirectory(dir);
    }
    return FSDirectory.open(dir);
  }
}
