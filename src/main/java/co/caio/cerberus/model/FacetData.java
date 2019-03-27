package co.caio.cerberus.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Map;
import org.immutables.value.Value;

@ImmutableStyle
@JsonSerialize(as = ImmutableFacetData.class)
@JsonDeserialize(as = ImmutableFacetData.class)
@Value.Immutable
public interface FacetData {
  String dimension();

  Map<String, Long> children();

  class Builder extends ImmutableFacetData.Builder {}
}
