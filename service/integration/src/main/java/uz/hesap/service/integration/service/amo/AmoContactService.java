package uz.hesap.service.integration.service.amo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.integration.domain.AmoContactEntity;
import uz.hesap.service.integration.domain.AmoLogEntity;
import uz.hesap.service.integration.model.AmoLogResponse;
import uz.hesap.service.integration.model.amo.AmoContactCommand;
import uz.hesap.service.integration.model.amo.AmoContactResponse;
import uz.hesap.service.integration.repository.AmoContactRepository;
import uz.hesap.service.integration.repository.AmoLogRepository;

// amoCRM kontakt yaratish (eski crm-app AmoContactService reaktiv varianti).
// POST /api/v4/contacts (Bearer token) — [{name, first_name, last_name, custom_fields_values}].
// custom fields: telefon (phone-field-id) + Hesap user id (hesap-user-id-field-id).
// Har urinish integration.amo_log'ga yoziladi (monitoring).
@Log4j2
@Service
public class AmoContactService {

  private final WebClient.Builder webClientBuilder;
  private final AmoAuthService authService;
  private final AmoContactRepository contactRepository;
  private final AmoLogRepository logRepository;
  private final ObjectMapper objectMapper;
  private final String contactsUrl;
  private final Long phoneFieldId;
  private final Long hesapUserIdFieldId;

  public AmoContactService(
      WebClient.Builder webClientBuilder,
      AmoAuthService authService,
      AmoContactRepository contactRepository,
      AmoLogRepository logRepository,
      ObjectMapper objectMapper,
      @Value("${application.amocrm.contacts-url}") String contactsUrl,
      @Value("${application.amocrm.phone-field-id}") Long phoneFieldId,
      @Value("${application.amocrm.hesap-user-id-field-id}") Long hesapUserIdFieldId) {
    this.webClientBuilder = webClientBuilder;
    this.authService = authService;
    this.contactRepository = contactRepository;
    this.logRepository = logRepository;
    this.objectMapper = objectMapper;
    this.contactsUrl = contactsUrl;
    this.phoneFieldId = phoneFieldId;
    this.hesapUserIdFieldId = hesapUserIdFieldId;
  }

  // Kontakt yaratish — bir foydalanuvchi uchun ikkinchi marta yaratilmaydi (idempotent).
  public Mono<AmoContactResponse> addContact(AmoContactCommand cmd) {
    if (cmd.hesapUserId() == null) {
      return Mono.error(new BadRequestException("hesapUserId majburiy"));
    }
    return contactRepository
        .findFirstByHesapUserId(cmd.hesapUserId())
        .map(existing -> new AmoContactResponse(existing.getAmoContactId()))
        .switchIfEmpty(Mono.defer(() -> createContact(cmd)));
  }

  // Monitoring loglari (paged).
  public Flux<AmoLogResponse> logs(int page, int size) {
    return logRepository
        .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
        .map(
            e ->
                new AmoLogResponse(
                    e.getId(),
                    e.getAction(),
                    e.getStatus(),
                    e.getUserId(),
                    e.getPhone(),
                    e.getAmoContactId(),
                    e.getRequest(),
                    e.getResponse(),
                    e.getErrorMessage(),
                    e.getCreatedAt()));
  }

  private Mono<AmoContactResponse> createContact(AmoContactCommand cmd) {
    String requestJson = buildBody(cmd).toString();
    return authService
        .accessToken()
        .flatMap(
            token ->
                webClientBuilder
                    .build()
                    .post()
                    .uri(contactsUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .bodyValue(buildBody(cmd))
                    .retrieve()
                    .onStatus(
                        HttpStatusCode::isError,
                        resp ->
                            resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(
                                    b -> {
                                      log.warn(
                                          "amoCRM contact create failed: {} {}",
                                          resp.statusCode(),
                                          b);
                                      return Mono.error(
                                          new BadRequestException(
                                              "amoCRM contact failed: " + resp.statusCode() + " " + b));
                                    }))
                    .bodyToMono(String.class))
        .flatMap(
            json ->
                extractContactId(json)
                    .flatMap(
                        amoId ->
                            saveMapping(cmd, amoId)
                                .then(
                                    writeLog(
                                        "CONTACT_CREATE", "SUCCESS", cmd, amoId, requestJson, json,
                                        null))
                                .thenReturn(new AmoContactResponse(amoId))))
        .onErrorResume(
            e ->
                writeLog("CONTACT_CREATE", "ERROR", cmd, null, requestJson, null, e.getMessage())
                    .then(Mono.error(e)));
  }

  // amoCRM POST tanasi: bitta kontaktli massiv.
  private ArrayNode buildBody(AmoContactCommand cmd) {
    ObjectNode contact = objectMapper.createObjectNode();
    contact.put("name", cmd.name());
    contact.put("first_name", cmd.firstName());
    contact.put("last_name", cmd.lastName());

    ArrayNode customFields = contact.putArray("custom_fields_values");
    if (cmd.phone() != null && !cmd.phone().isBlank()) {
      ObjectNode phoneField = customFields.addObject();
      phoneField.put("field_id", phoneFieldId);
      ObjectNode phoneValue = phoneField.putArray("values").addObject();
      phoneValue.put("value", cmd.phone());
      phoneValue.put("enum_code", "WORK");
    }
    ObjectNode hesapField = customFields.addObject();
    hesapField.put("field_id", hesapUserIdFieldId);
    hesapField.putArray("values").addObject().put("value", cmd.hesapUserId().toString());

    ArrayNode body = objectMapper.createArrayNode();
    body.add(contact);
    return body;
  }

  private Mono<Long> extractContactId(String json) {
    try {
      JsonNode contacts = objectMapper.readTree(json).path("_embedded").path("contacts");
      if (contacts.isArray() && !contacts.isEmpty()) {
        long id = contacts.get(0).path("id").asLong();
        log.info("amoCRM contact created: {}", id);
        return Mono.just(id);
      }
      return Mono.error(new BadRequestException("amoCRM javobida kontakt id yo'q"));
    } catch (Exception e) {
      return Mono.error(new BadRequestException("amoCRM javobini o'qib bo'lmadi"));
    }
  }

  private Mono<AmoContactEntity> saveMapping(AmoContactCommand cmd, Long amoId) {
    AmoContactEntity entity = new AmoContactEntity();
    entity.setAmoContactId(amoId);
    entity.setName(cmd.name());
    entity.setFirstName(cmd.firstName());
    entity.setLastName(cmd.lastName());
    entity.setPhone(cmd.phone());
    entity.setHesapUserId(cmd.hesapUserId());
    return contactRepository.save(entity);
  }

  // Monitoring log yozish — asosiy oqimni buzmaydi (xatosi yutiladi).
  private Mono<Void> writeLog(
      String action,
      String status,
      AmoContactCommand cmd,
      Long amoContactId,
      String request,
      String response,
      String errorMessage) {
    AmoLogEntity entity = new AmoLogEntity();
    entity.setAction(action);
    entity.setStatus(status);
    entity.setUserId(cmd.hesapUserId() != null ? cmd.hesapUserId().toString() : null);
    entity.setPhone(cmd.phone());
    entity.setAmoContactId(amoContactId);
    entity.setRequest(request);
    entity.setResponse(response);
    entity.setErrorMessage(errorMessage);
    return logRepository.save(entity).then().onErrorResume(e -> Mono.empty());
  }
}
