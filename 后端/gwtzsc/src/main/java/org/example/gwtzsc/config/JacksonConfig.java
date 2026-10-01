package org.example.gwtzsc.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

/**
 * Web 层 Jackson 配置。
 *
 * 1. 注册 JavaTimeModule，使 LocalDateTime 等以 ISO 字符串序列化
 *    （WRITE_DATES_AS_TIMESTAMPS 关闭）。
 * 2. 雪花主键是 19 位 Long，超出 JavaScript Number.MAX_SAFE_INTEGER(2^53-1)，
 *    前端以 number 解析会丢失精度导致详情"商品不存在"。这里将超过安全范围的
 *    Long 序列化为字符串（小数字如 views/balance 仍保留 number）。
 *
 * 直接提供 @Primary ObjectMapper（而非 customizer），确保一定生效。
 */
@Configuration
public class JacksonConfig {

    private static final long JS_SAFE_MAX = 9007199254740991L; // 2^53 - 1

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        SimpleModule module = new SimpleModule("Long-As-String-When-Unsafe");
        module.addSerializer(Long.class, new UnsafeLongSerializer());
        module.addSerializer(Long.TYPE, new UnsafeLongSerializer());
        module.addDeserializer(Long.class, new UnsafeLongDeserializer());
        module.addDeserializer(Long.TYPE, new UnsafeLongDeserializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    /** 超过 JS 安全整数的 Long 序列化为字符串，否则保留数值。 */
    public static class UnsafeLongSerializer extends StdSerializer<Long> {
        public UnsafeLongSerializer() { super(Long.class); }

        @Override
        public void serialize(Long value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else if (value.longValue() <= JS_SAFE_MAX && value.longValue() >= -JS_SAFE_MAX) {
                gen.writeNumber(value.longValue());
            } else {
                gen.writeString(value.toString());
            }
        }
    }

    /** 反序列化：字符串或数字都转回 Long。 */
    public static class UnsafeLongDeserializer extends StdDeserializer<Long> {
        public UnsafeLongDeserializer() { super(Long.class); }

        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            if (p.currentToken().isNumeric()) {
                return p.getLongValue();
            }
            String text = p.getText();
            if (text == null || text.trim().isEmpty()) return null;
            return Long.parseLong(text.trim());
        }
    }
}
