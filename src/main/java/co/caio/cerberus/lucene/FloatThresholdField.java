package co.caio.cerberus.lucene;

import org.apache.lucene.facet.taxonomy.FloatAssociationFacetField;

// Simple wrapper class to avoid nested path
// i.e. the base class allows us to have a facet like:
//  "score"->"popularity"->0.42f
// but I want to guarantee that nesting isn't allowed:
//  "score_popularity"->0.42f
public class FloatThresholdField extends FloatAssociationFacetField {
  public FloatThresholdField(float threshold, String dim, String path) {
    super(threshold, dim, path);
    if (threshold < 0 || threshold > 1) {
      throw new IllegalArgumentException(
          String.format("The threshold argument should be [0,1] (Got %f)", threshold));
    }
  }
}
