package co.caio.cerberus.lucene;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsCollector.MatchingDocs;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.IntTaxonomyFacets;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BytesRef;

public class FloatAssociationsThresholdCount extends IntTaxonomyFacets {

  private final Map<String, Float> labelToThreshold;

  public FloatAssociationsThresholdCount(
      String indexFieldName,
      final Map<String, Float> _labelToThreshold,
      TaxonomyReader taxoReader,
      FacetsConfig config,
      FacetsCollector fc)
      throws IOException {
    super(indexFieldName, taxoReader, config, fc);

    if (_labelToThreshold == null) {
      throw new NullPointerException("_labelToThreshold must not be null");
    }

    labelToThreshold = _labelToThreshold;
    computeValues(fc.getMatchingDocs());
  }

  private float threshold(int ord) throws IOException {
    var components = taxoReader.getPath(ord).components;
    assert components.length == 2;
    return labelToThreshold.getOrDefault(components[1], 1.0f);
  }

  private void computeValues(List<MatchingDocs> matchingDocs) throws IOException {
    for (MatchingDocs hits : matchingDocs) {
      BinaryDocValues dv = hits.context.reader().getBinaryDocValues(indexFieldName);
      if (dv == null) {
        continue;
      }

      DocIdSetIterator docs = hits.bits.iterator();

      int doc;
      while ((doc = docs.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
        if (dv.docID() < doc) {
          dv.advance(doc);
        }
        if (dv.docID() == doc) {
          final BytesRef bytesRef = dv.binaryValue();
          byte[] bytes = bytesRef.bytes;
          int end = bytesRef.offset + bytesRef.length;
          int offset = bytesRef.offset;
          while (offset < end) {
            int ord =
                ((bytes[offset] & 0xFF) << 24)
                    | ((bytes[offset + 1] & 0xFF) << 16)
                    | ((bytes[offset + 2] & 0xFF) << 8)
                    | (bytes[offset + 3] & 0xFF);
            offset += 4;
            int value =
                ((bytes[offset] & 0xFF) << 24)
                    | ((bytes[offset + 1] & 0xFF) << 16)
                    | ((bytes[offset + 2] & 0xFF) << 8)
                    | (bytes[offset + 3] & 0xFF);
            offset += 4;

            if (Float.intBitsToFloat(value) >= threshold(ord)) {
              increment(ord);
            }
          }
        }
      }
    }
  }
}
