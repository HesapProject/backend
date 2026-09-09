package uz.hesap.service.integration.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.integration.domain.SmsInviteTemplateEntity;
import uz.hesap.service.integration.model.SendInviteRequest;
import uz.hesap.service.integration.model.SendInviteResponse;
import uz.hesap.service.integration.model.SmsInviteTemplateRequest;
import uz.hesap.service.integration.model.SmsInviteTemplateResponse;
import uz.hesap.service.integration.repository.SmsInviteTemplateRepository;

// Per-til taklif SMS shablonlari + Eskiz orqali taklif yuborish.
@Log4j2
@Service
@RequiredArgsConstructor
public class SmsInviteService {

  private final SmsInviteTemplateRepository repository;
  private final EskizProvider eskizProvider;

  public Flux<SmsInviteTemplateResponse> getTemplates() {
    return repository
        .findAll()
        .map(e -> new SmsInviteTemplateResponse(e.getLanguage(), e.getText()));
  }

  public Flux<SmsInviteTemplateResponse> saveTemplates(
      final List<SmsInviteTemplateRequest> requests) {
    return Flux.fromIterable(requests == null ? List.of() : requests)
        .filter(r -> r != null && r.language() != null)
        .concatMap(this::saveTemplate);
  }

  private Mono<SmsInviteTemplateResponse> saveTemplate(final SmsInviteTemplateRequest request) {
    final String lang = normalize(request.language());
    return repository
        .findByLanguage(lang)
        .defaultIfEmpty(newEntity(lang))
        .flatMap(
            e -> {
              e.setLanguage(lang);
              e.setText(request.text());
              return repository.save(e);
            })
        .map(e -> new SmsInviteTemplateResponse(e.getLanguage(), e.getText()));
  }

  // Tanlangan til shabloni bo'yicha Eskiz orqali SMS yuboradi.
  public Mono<SendInviteResponse> sendInvite(final SendInviteRequest request) {
    if (request.phone() == null || request.phone().isBlank()) {
      return Mono.error(new BadRequestException("Telefon raqam kerak"));
    }
    final String lang = normalize(request.language());
    return repository
        .findByLanguage(lang)
        .switchIfEmpty(
            Mono.error(new BadRequestException("Bu til uchun SMS shablon topilmadi: " + lang)))
        .flatMap(
            t -> {
              if (t.getText() == null || t.getText().isBlank()) {
                return Mono.error(
                    new BadRequestException("Bu til uchun SMS shablon bo'sh: " + lang));
              }
              return eskizProvider
                  .send(request.phone(), t.getText())
                  .map(r -> new SendInviteResponse("SENT", "SMS yuborildi"));
            });
  }

  private SmsInviteTemplateEntity newEntity(final String lang) {
    SmsInviteTemplateEntity e = new SmsInviteTemplateEntity();
    e.setLanguage(lang);
    return e;
  }

  private String normalize(final String lang) {
    if (lang == null) {
      return "uz";
    }
    String l = lang.trim().toLowerCase();
    return l.equals("en") ? "eng" : l;
  }
}
