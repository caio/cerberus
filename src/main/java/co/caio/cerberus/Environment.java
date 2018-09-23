package co.caio.cerberus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class Environment {
  private static ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

  public static ObjectMapper getObjectMapper() {
    return mapper;
  }
}
