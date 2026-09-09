package uz.hesap.service.log.context;

import io.r2dbc.spi.ConnectionFactory;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uz.hesap.service.log.util.JsonToListConverter;
import uz.hesap.service.log.util.JsonToMapConverter;
import uz.hesap.service.log.util.ListToJsonConverter;
import uz.hesap.service.log.util.MapToJsonConverter;

@Configuration
@EnableR2dbcAuditing
@EnableR2dbcRepositories
public class ReactivePostgresConfig {
  private final Jackson2ObjectMapperBuilder objectMapper;

  public ReactivePostgresConfig(Jackson2ObjectMapperBuilder objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Bean
  public R2dbcCustomConversions r2dbcCustomConversions(ConnectionFactory connectionFactory) {
    List<Converter<?, ?>> converters = new ArrayList<>();
    converters.add(new JsonToMapConverter(objectMapper));
    converters.add(new MapToJsonConverter(objectMapper));
    converters.add(new ListToJsonConverter<>(objectMapper));
    converters.add(new JsonToListConverter<>(objectMapper));
    return new R2dbcCustomConversions(getStoreConversions(connectionFactory), converters);
  }

  @Bean
  protected CustomConversions.StoreConversions getStoreConversions(
      ConnectionFactory connectionFactory) {
    R2dbcDialect dialect = getDialect(connectionFactory);
    List<Object> converters = new ArrayList<>(dialect.getConverters());
    converters.addAll(R2dbcCustomConversions.STORE_CONVERTERS);
    return CustomConversions.StoreConversions.of(dialect.getSimpleTypeHolder(), converters);
  }

  @Bean
  public R2dbcDialect getDialect(ConnectionFactory connectionFactory) {
    return DialectResolver.getDialect(connectionFactory);
  }
}
