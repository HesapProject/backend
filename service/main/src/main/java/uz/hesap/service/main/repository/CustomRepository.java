package uz.hesap.service.main.repository;

import io.r2dbc.spi.Row;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.InvalidArgumentException;
import uz.hesap.service.common.util.enums.Role;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.domain.UserEntity;
import uz.hesap.service.main.model.UserWithRoleDto;
import uz.hesap.service.main.util.Constants;
import uz.hesap.service.main.util.Utils;

@Repository
@RequiredArgsConstructor
public class CustomRepository {
  private final DatabaseClient databaseClient;

  public Flux<UserEntity> getUsers(final String search, Pageable pageable) {
    StringBuilder query = new StringBuilder();
    query.append(
        "SELECT * FROM "
            + Constants.SCHEMA
            + "."
            + Constants.TABLE_USER
            + " WHERE deleted = false");

    if (search != null) {
      query.append(
          " AND (first_name ilike :search OR "
              + "last_name ilike :search OR "
              + "(first_name || ' ' || last_name) ilike :search OR "
              + "(last_name || ' ' || first_name) ilike :search OR "
              + "phone ilike :search OR "
              + "email ilike :search)");
    }

    if (pageable != null) {
      if (Objects.equals(pageable.getSort().isSorted(), Boolean.TRUE)) {
        query.append(" ORDER BY ");
        String sorting =
            pageable.getSort().stream()
                .map(order -> order.getProperty() + " " + (order.isAscending() ? "asc" : "desc"))
                .collect(Collectors.joining(", "));
        query.append(Utils.toSnakeCase(sorting));
      }
      query.append(" limit :limit offset :offset");
    }
    var spec = this.databaseClient.sql(query.toString());
    if (search != null) {
      spec = spec.bind("search", "%" + search + "%");
    }
    if (pageable != null) {
      spec = spec.bind("limit", pageable.getPageSize());
      spec = spec.bind("offset", pageable.getOffset());
    }
    return spec.map((row, metadata) -> mapRowToUserEntity(row)).all();
  }

  public Mono<Long> getUsersCount(final String search) {
    StringBuilder query = new StringBuilder();
    query.append(
        "SELECT count(*) FROM "
            + Constants.SCHEMA
            + "."
            + Constants.TABLE_USER
            + " WHERE deleted = false");

    if (search != null) {
      query.append(
          " AND (first_name ilike :search OR "
              + "last_name ilike :search OR "
              + "(first_name || ' ' || last_name) ilike :search OR "
              + "(last_name || ' ' || first_name) ilike :search OR "
              + "phone ilike :search OR "
              + "email ilike :search)");
    }
    var spec = this.databaseClient.sql(query.toString());

    if (search != null) {
      spec = spec.bind("search", "%" + search + "%");
    }
    return spec.map(row -> row.get(0, Long.class)).one();
  }

  // Kompaniya xodimlari (staff a'zolari) — user detali + role (staff.type).
  // MANAGER -> ADMIN (Role enum'da MANAGER yo'q). Company/user_company o'rniga staff.
  public Flux<UserWithRoleDto> getCompanyUsers(
      final UUID companyId, final String search, final Pageable pageable) {
    StringBuilder query = new StringBuilder();
    query.append(
        "SELECT u.id, s.id AS staff_id, u.first_name, u.last_name, u.phone, u.email,"
            + " CASE WHEN s.type = 'OWNER' THEN 'OWNER' ELSE 'ADMIN' END AS role,"
            + " u.created_date, u.last_modified_date FROM "
            + Constants.SCHEMA
            + ".staff s JOIN "
            + Constants.SCHEMA
            + "."
            + Constants.TABLE_USER
            + " u ON u.id = s.user_id AND u.deleted = false"
            + " WHERE s.company_id = :companyId AND s.deleted = false");
    if (search != null) {
      query.append(
          " AND (u.first_name ilike :search OR u.last_name ilike :search OR"
              + " (u.first_name || ' ' || u.last_name) ilike :search OR u.phone ilike :search)");
    }
    query.append(" ORDER BY s.created_date DESC");
    if (pageable != null) {
      query.append(" limit :limit offset :offset");
    }
    var spec = this.databaseClient.sql(query.toString()).bind("companyId", companyId);
    if (search != null) {
      spec = spec.bind("search", "%" + search + "%");
    }
    if (pageable != null) {
      spec = spec.bind("limit", pageable.getPageSize()).bind("offset", pageable.getOffset());
    }
    return spec.map((row, metadata) -> mapRowToUserWithRoleDto(row)).all();
  }

  public Mono<Long> getCompanyUsersCount(final UUID companyId, final String search) {
    StringBuilder query = new StringBuilder();
    query.append(
        "SELECT count(*) FROM "
            + Constants.SCHEMA
            + ".staff s JOIN "
            + Constants.SCHEMA
            + "."
            + Constants.TABLE_USER
            + " u ON u.id = s.user_id AND u.deleted = false"
            + " WHERE s.company_id = :companyId AND s.deleted = false");
    if (search != null) {
      query.append(
          " AND (u.first_name ilike :search OR u.last_name ilike :search OR"
              + " (u.first_name || ' ' || u.last_name) ilike :search OR u.phone ilike :search)");
    }
    var spec = this.databaseClient.sql(query.toString()).bind("companyId", companyId);
    if (search != null) {
      spec = spec.bind("search", "%" + search + "%");
    }
    return spec.map(row -> row.get(0, Long.class)).one();
  }

  private UserEntity mapRowToUserEntity(Row row) {
    UserEntity entity = new UserEntity();
    entity.setId(row.get("id", UUID.class));
    entity.setFirstName(row.get("first_name", String.class));
    entity.setLastName(row.get("last_name", String.class));
    entity.setMidName(row.get("mid_name", String.class));
    entity.setPhone(row.get("phone", String.class));
    entity.setEmail(row.get("email", String.class));
    // `in` (PINFL/STIR) — admin ro'yxati detalga shu identifikator bilan o'tadi.
    entity.setIn(row.get("pinfl", String.class));
    entity.setTin(row.get("tin", String.class));
    entity.setType(mapEnum(UserType.class, row.get("type", String.class)));
    //    entity.setRole(mapEnum(Role.class, row.get("role", String.class)));
    entity.setCreatedDate(getInstant(row, "created_date"));
    entity.setLastModifiedDate(getInstant(row, "last_modified_date"));
    return entity;
  }

  private UserWithRoleDto mapRowToUserWithRoleDto(Row row) {

    return new UserWithRoleDto(
        row.get("id", UUID.class),
        row.get("staff_id", UUID.class),
        row.get("first_name", String.class),
        row.get("last_name", String.class),
        row.get("phone", String.class),
        row.get("email", String.class),
        mapEnum(Role.class, row.get("role", String.class)),
        getInstant(row, "created_date"),
        getInstant(row, "last_modified_date"));
  }

  private <E extends Enum<E>> E mapEnum(Class<E> enumClass, String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Enum.valueOf(enumClass, value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new InvalidArgumentException("Invalid enum value: " + value, e);
    }
  }

  private Instant getInstant(Row row, String columnName) {
    LocalDateTime localDateTime = row.get(columnName, LocalDateTime.class);
    return localDateTime != null ? localDateTime.toInstant(ZoneOffset.UTC) : null;
  }

  public Flux<String> findAllFcmTokens(UUID userId) {
    String query =
        "SELECT fcm_token FROM "
            + Constants.SCHEMA
            + "."
            + Constants.TABLE_SESSION
            + " WHERE user_id = :userId AND archived = false AND fcm_token IS NOT NULL";
    return databaseClient
        .sql(query)
        .bind("userId", userId)
        .map(row -> row.get("fcm_token", String.class))
        .all();
  }
}
