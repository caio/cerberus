package co.caio.cerberus.service;

import co.caio.cerberus.Environment;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;

public class Serializer {
  private static final ObjectMapper mapper = Environment.getObjectMapper();

  public static String encode(Object object) throws Exception {
    return mapper.writeValueAsString(object);
  }

  public static Future<String> encodeAsync(Object object) {
    return Future.future(
        fut -> {
          try {
            fut.complete(encode(object));
          } catch (Exception e) {
            fut.fail(e);
          }
        });
  }

  public static <T> T decode(String data, Class<T> clazz) throws Exception {
    return mapper.readValue(data, clazz);
  }

  public static Future<?> decodeAsync(String data, Class<?> clazz) {
    return Future.future(
        fut -> {
          try {
            fut.complete(decode(data, clazz));
          } catch (Exception e) {
            fut.fail(e);
          }
        });
  }
}
