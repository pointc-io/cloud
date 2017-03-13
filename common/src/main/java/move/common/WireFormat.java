package move.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javaslang.collection.List;
import javaslang.jackson.datatype.JavaslangModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.*;

/**
 *
 */
public class WireFormat {
    public static final Logger LOG = LoggerFactory.getLogger(WireFormat.class);
    public static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaslangModule());
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        MAPPER.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        MAPPER.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);

        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, true);
        MAPPER.configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, true);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static void main(String[] args) {
        System.out.println(WireFormat.stringify(new MyMessage()));

    }

    public static <T> T clone(T value) {
        if (value == null) {
            return null;
        }

        return parse((Class<T>)value.getClass(), byteify(value));
    }

    public static <T> T parse(Class<T> cls, String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, cls);
        } catch (Throwable e) {
            LOG.info("Failed to parse Json string", e);
//            throw new RuntimeException(e);
            return null;
        }
    }

    public static <T> T parse(Class<T> cls, byte[] json) {
        if (json == null || json.length == 0) {
            return null;
        }
        try {
            return MAPPER.readValue(json, cls);
        } catch (Throwable e) {
            LOG.info("Failed to parse Json data", e);
//            throw new RuntimeException(e);
            return null;
        }
    }

    public static <T> T parse(Class<T> cls, byte[] json, int offset, int length) {
        if (length == 0) {
            return null;
        }
        try {
            return MAPPER.readValue(json, offset, length, cls);
        } catch (Throwable e) {
            LOG.info("Failed to parse Json data", e);
//            throw new RuntimeException(e);
            return null;
        }
    }

    public static <T> T parse(Class<T> cls, InputStream json) {
        try {
            return MAPPER.readValue(json, cls);
        } catch (Throwable e) {
            LOG.info("Failed to parse Json data", e);
//            throw new RuntimeException(e);
            return null;
        }
    }

    public static byte[] byteify(Object obj) {
        try {
            return MAPPER.writeValueAsBytes(obj);
        } catch (Throwable e) {
            LOG.info("Failed to byteify Object", e);
//            throw new RuntimeException(e);
            return null;
        }
    }

    public static String stringify(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Throwable e) {
            LOG.info("Failed to stringify Object", e);
//            throw new RuntimeException(e);
            return null;
        }
    }

    public static class MyMessage {
        @JsonProperty
        List<String> myList = List.of("1", "2", "3");
        @JsonProperty
        LocalDate localDate = LocalDate.now();
        @JsonProperty
        LocalTime localTime = LocalTime.now();
        @JsonProperty
        LocalDateTime localDateTime = LocalDateTime.now();
        @JsonProperty
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("PST", ZoneId.SHORT_IDS));

        @JsonProperty
        Duration duration = Duration.ofDays(2);
        @JsonProperty
        Instant instant = Instant.now(Clock.systemDefaultZone());
    }
}