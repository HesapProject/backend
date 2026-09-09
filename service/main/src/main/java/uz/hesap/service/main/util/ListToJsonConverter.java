package uz.hesap.service.main.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@WritingConverter
public class ListToJsonConverter<T> implements Converter<List<T>, Json> {

  private static final Logger logger = LogManager.getLogger("ListToJsonConverter");
  private final ObjectMapper objectMapper;

  public ListToJsonConverter(Jackson2ObjectMapperBuilder objectMapper) {
    this.objectMapper = objectMapper.build();
  }

  @Override
  public Json convert(List<T> source) {
    try {
      return Json.of(objectMapper.writeValueAsString(source));
    } catch (JsonProcessingException e) {
      logger.error("Error occurred while serializing map to JSON: {}", source, e);
    }
    return Json.of("");
  }
}
