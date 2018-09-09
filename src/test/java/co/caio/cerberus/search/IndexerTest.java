package co.caio.cerberus.search;

import co.caio.cerberus.Util;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class IndexerTest {
    @Test
    public void badUsage() {
        var exc = Indexer.IndexBuilderException.class;
        var builder = new Indexer.Builder();
        assertAll("index builder",
                () -> assertThrows(exc, builder::build),
                () -> assertThrows(exc, () -> builder.reset().createMode().build()),
                () -> assertThrows(exc, () -> builder.reset().analyzer(new StandardAnalyzer()).build()),
                () -> assertThrows(exc, () -> builder.reset().dataDirectory(Paths.get("void")).createMode().build()),
                () -> assertThrows(exc, () -> builder.reset().inMemory().appendMode().build())
        );
    }

    @Test
    public void simpleLocalIndexer() throws IOException {
        var tempDir = Files.createTempDirectory("cerberus-test");
        var index = new Indexer.Builder().dataDirectory(tempDir).createOrAppendMode().build();
        assertEquals(0, index.numDocs());
        index.addRecipe(Util.getBasicRecipe());
        assertEquals(1, index.numDocs());
        index.close();

        // Reopening it should still allow us to read its documents
        var newIndexSameDir = new Indexer.Builder().dataDirectory(tempDir).appendMode().build();
        assertEquals(1, newIndexSameDir.numDocs());
        newIndexSameDir.close();

        // But opening should erase the old data
        var destructiveIndex = new Indexer.Builder().dataDirectory(tempDir).createMode().build();
        assertEquals(0, destructiveIndex.numDocs());
    }
}