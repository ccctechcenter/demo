package com.ccctc.adaptor.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper jacksonObjectMapper() {


        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);

        //
        // Local Date/Times will be serialized as text, whereas the old Java dates are serialized as a timestamp.
        // If we set WRITE_DATES_AS_TIMESTAMPS to false, we can remove all these extra serializers as they will do
        // this automatically
        //
        // Examples:
        // LocalDate = 2018-05-01
        // LocalTime = 10:15:30
        // LocalDateTime = 2018-05-01T10:15:30
        //
        // To ensure backwards compatibility, old Java Date objects will continue be serialized as a timestamp
        //

        // @TODO - remove these new serializers if/when we set WRITE_DATES_AS_TIMESTAMPS  to false
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addSerializer(LocalTime.class, new LocalTimeSerializer());
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        mapper.registerModule(module);

        return mapper;
    }

    // @TODO - remove these new serializers if/when we set WRITE_DATES_AS_TIMESTAMPS  to false

    public static class LocalDateSerializer extends StdSerializer<LocalDate> {

        private static final long serialVersionUID = 1L;

        public LocalDateSerializer(){
            super(LocalDate.class);
        }

        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider sp) throws IOException, JsonProcessingException {
            gen.writeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
    }

    public static class LocalTimeSerializer extends StdSerializer<LocalTime> {

        private static final long serialVersionUID = 1L;

        public LocalTimeSerializer(){
            super(LocalTime.class);
        }

        @Override
        public void serialize(LocalTime value, JsonGenerator gen, SerializerProvider sp) throws IOException, JsonProcessingException {
            gen.writeString(value.format(DateTimeFormatter.ISO_LOCAL_TIME));
        }
    }

    public static class LocalDateTimeSerializer extends StdSerializer<LocalDateTime> {

        private static final long serialVersionUID = 1L;

        public LocalDateTimeSerializer(){
            super(LocalDateTime.class);
        }

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider sp) throws IOException, JsonProcessingException {
            gen.writeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }
}
