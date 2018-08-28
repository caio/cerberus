package co.caio.cerberus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class Environment {
    private static ObjectMapper mapper = null;

    public static ObjectMapper getObjectMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module());
        }
        return mapper;
    }
}
