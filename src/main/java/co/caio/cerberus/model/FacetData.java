package co.caio.cerberus.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.value.Value;

@ImmutableStyle
@JsonSerialize(as = ImmutableFacetData.class)
@JsonDeserialize(as = ImmutableFacetData.class)
@Value.Immutable
public interface FacetData {
  String dimension();

  List<LabelData> children();

  class Builder extends ImmutableFacetData.Builder {
    public Builder addChild(String label, long count) {
      addChildren(LabelData.of(label, count));
      return this;
    }
  }

  @Value.Immutable(builder = false)
  @JsonFormat(shape = JsonFormat.Shape.ARRAY)
  @JsonPropertyOrder({"label", "count"})
  @JsonSerialize(as = ImmutableLabelData.class)
  @JsonDeserialize(as = ImmutableLabelData.class)
  interface LabelData {
    @Value.Parameter
    String label();

    @Value.Parameter
    long count();

    static LabelData of(String label, long count) {
      return ImmutableLabelData.of(label, count);
    }
  }
}
