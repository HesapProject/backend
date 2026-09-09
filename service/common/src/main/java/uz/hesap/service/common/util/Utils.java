package uz.hesap.service.common.util;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import uz.hesap.service.common.exception.InvalidOperationException;

public class Utils {
  public static List<String> getDifferentFields(
      Map<String, Object> model1, Map<String, Object> model2) {
    if (model1 == null || model2 == null) {
      return Collections.emptyList(); // Null bo‘lsa, bo‘sh list qaytariladi
    }
    List<String> differentFields = new ArrayList<>();
    for (Map.Entry<String, Object> entry : model1.entrySet()) {
      String field = entry.getKey();
      if (!Objects.equals(entry.getValue(), model2.get(field))) {
        differentFields.add(field);
      }
    }
    return differentFields;
  }

  public static <T> HashMap<UUID, T> mapById(List<T> list, Function<T, UUID> idExtractor) {
    HashMap<UUID, T> map = new HashMap<>();
    list.forEach(item -> map.put(idExtractor.apply(item), item));
    return map;
  }

  public static <T> HashMap<String, T> mapByIdString(
      List<T> list, Function<T, String> idExtractor) {
    HashMap<String, T> map = new HashMap<>();
    list.forEach(item -> map.put(idExtractor.apply(item), item));
    return map;
  }

  public static <T> List<UUID> idList(List<T> list, Function<T, UUID> idExtractor) {
    return list.stream().map(idExtractor).collect(Collectors.toSet()).stream().toList();
  }

  public static <T> Set<UUID> idSet(List<T> list, Function<T, UUID> idExtractor) {
    return list.stream().map(idExtractor).collect(Collectors.toSet());
  }

  public static <T> List<String> idListStr(List<T> list, Function<T, String> idExtractor) {
    return list.stream().map(idExtractor).collect(Collectors.toSet()).stream().toList();
  }

  public static <E extends Enum<E>> List<String> nameEnums(final Collection<E> enums) {
    return enums.stream().map(Enum::name).toList();
  }

  public static <T extends String> T getNotNull(T t1, T t2) {
    if (t1 == null || t1.isEmpty()) return t2;
    return t1;
  }

  public static String getEmptyStr(String str) {
    if (str == null) return "";
    return str;
  }

  public static <T> T getNotNull(T t1, T t2) {
    if (t1 == null) return t2;
    return t1;
  }

  public static <T> T getNotNull(T t1, T t2, T t3) {
    if (t1 == null) return getNotNull(t2, t3);
    return t1;
  }

  public static <E> boolean isEmpty(final Collection<E> elements) {
    return elements == null || elements.isEmpty();
  }

  public static <E> boolean isNotEmpty(final Collection<E> elements) {
    return elements != null && !elements.isEmpty();
  }

  public static boolean isEmpty(final String str) {
    return str == null || str.isEmpty();
  }

  public static void checkCompanyId(final UserPrincipal userPrincipal, final UUID companyId) {
    if (!userPrincipal.user().company().id().equals(companyId)) {
      throw new InvalidOperationException("You are not allowed to do this operation");
    }
  }
}
