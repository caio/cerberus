package co.caio.cerberus.lucene;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

public class TextFieldWithVectors extends Field {
  // Tokenized, with vectors, not stored
  public static final FieldType TYPE;

  static {
    TYPE = new FieldType();
    TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
    TYPE.setTokenized(true);
    TYPE.setStoreTermVectors(true);
    TYPE.freeze();
  }

  public TextFieldWithVectors(String name, String value) {
    super(name, value, TYPE);
  }
}
