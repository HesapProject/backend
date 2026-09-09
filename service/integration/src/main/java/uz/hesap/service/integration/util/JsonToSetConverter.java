package uz.hesap.service.integration.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import java.io.IOException;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@ReadingConverter
public class JsonToSetConverter<T> implements Converter<Json, Set<T>> {

  private static final Logger logger = LogManager.getLogger("JsonToSetConverter");

  private final ObjectMapper objectMapper;

  public JsonToSetConverter(Jackson2ObjectMapperBuilder objectMapper) {
    this.objectMapper = objectMapper.build();
  }

  @Override
  public Set<T> convert(Json json) {
    try {
      return objectMapper.readValue(json.asString(), new TypeReference<>() {});
    } catch (IOException e) {
      logger.error("Problem while parsing JSON: {}", json, e);
    }
    return new HashSet<>() {};
  }
}
