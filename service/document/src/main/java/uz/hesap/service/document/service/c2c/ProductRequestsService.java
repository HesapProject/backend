package uz.hesap.service.document.service.c2c;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.exception.InvalidOperationException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.document.ProductRequestEntity;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.ProductRequestStatus;
import uz.hesap.service.document.mapper.ProductRequestMapper;
import uz.hesap.service.document.model.request.ProductRequestCreateRequest;
import uz.hesap.service.document.model.response.ProductRequestResponse;
import uz.hesap.service.document.repository.ProductRequestRepository;
import uz.hesap.service.document.service.document.DocumentQueryHelper;

// Mahsulot so'rovlari: yaratish / tasdiq / rad / bekor / ro'yxat.
// Cancel so'rovidek struktura, lekin approve/reject faqat so'rov statusini o'zgartiradi
// (shartnomaga tegmaydi).
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRequestsService {

  private final ProductRequestRepository productRequestRepository;
  private final DocumentQueryHelper documentQueryHelper;
  private final ProductRequestMapper productRequestMapper;
  private final ContractCompletionService contractCompletionService;
  private final RoumingFacturaService roumingFacturaService;

  // ================ CREATE ================

  @Transactional
  public Mono<Void> create(UserPrincipal user, ProductRequestCreateRequest request) {
    UUID userId = user.user().id();
    String requesterIn = user.user().identifier();
    return documentQueryHelper
        .findAndValidateParty(request.contractId(), userId)
        .flatMap(this::requireActive)
        .flatMap(
            doc ->
                assertNoPending(request.productId())
                    .then(
                        save(
                            doc,
                            request.productId(),
                            requesterIn,
                            request.quantity(),
                            request.amount(),
                            request.reason())))
        .then();
  }

  // Shu mahsulot bo'yicha kutilayotgan (PENDING) so'rov bor bo'lsa — yangisini
  // yuborib bo'lmaydi. Avval o'zi bekor qilishi yoki qarshi taraf rad etishi kerak.
  private Mono<Void> assertNoPending(UUID productId) {
    return productRequestRepository
        .findAllByProductIdAndStatusAndDeletedFalse(productId, ProductRequestStatus.PENDING)
        .hasElements()
        .flatMap(
            exists ->
                Boolean.TRUE.equals(exists)
                    ? Mono.error(
                        new InvalidOperationException(
                            "Bu mahsulot bo'yicha kutilayotgan so'rov bor — avval uni bekor"
                                + " qiling yoki qarshi taraf javob bersin"))
                    : Mono.empty());
  }

  private Mono<ProductRequestEntity> save(
      DocumentEntity doc,
      UUID productId,
      String requesterIn,
      Double quantity,
      Double amount,
      String reason) {
    ProductRequestEntity entity = new ProductRequestEntity();
    entity.setContractId(doc.getId());
    entity.setProductId(productId);
    entity.setBuyerIn(doc.getBuyerIn());
    entity.setSellerIn(doc.getSellerIn());
    entity.setRequesterIn(requesterIn);
    entity.setStatus(ProductRequestStatus.PENDING);
    entity.setQuantity(quantity); // berilgan soni; null → to'liq deb qaraladi
    entity.setAmount(amount); // legacy (summa)
    entity.setReason(reason);
    return productRequestRepository.save(entity);
  }

  // ================ APPROVE ================

  @Transactional
  public Mono<Void> approve(UserPrincipal user, UUID id) {
    UUID userId = user.user().id();
    return pending(id)
        .flatMap(
            pr ->
                documentQueryHelper
                    .findAndValidateParty(pr.getContractId(), userId)
                    .flatMap(this::requireActive)
                    .flatMap(
                        doc -> {
                          // So'rovni yaratgan taraf o'zi tasdiqlay olmaydi.
                          if (user.user().identifier() != null
                              && user.user().identifier().equals(pr.getRequesterIn())) {
                            return Mono.<ProductRequestEntity>error(
                                new ForbiddenException(
                                    "So'rovni yaratuvchi o'zi tasdiqlay olmaydi"));
                          }
                          pr.setStatus(ProductRequestStatus.APPROVED);
                          // Tasdiqlangach: (1) Rouming'da draft schyot-faktura — xato
                          // asosiy oqimni buzmasin (faqat log); (2) shartnoma to'liq
                          // to'langan va barcha mahsulot topshirilgan bo'lsa avto yopiladi.
                          return productRequestRepository
                              .save(pr)
                              .flatMap(
                                  saved ->
                                      roumingFacturaService
                                          .sendDraftForApprovedRequest(saved, doc)
                                          .onErrorResume(
                                              e -> {
                                                log.warn(
                                                    "Rouming factura draft failed for pr {}: {}",
                                                    saved.getId(),
                                                    e.getMessage());
                                                return Mono.empty();
                                              })
                                          .thenReturn(saved))
                              .then(contractCompletionService.completeIfDone(pr.getContractId()));
                        }))
        .then();
  }

  // ================ REJECT ================

  @Transactional
  public Mono<Void> reject(UserPrincipal user, UUID id) {
    UUID userId = user.user().id();
    return pending(id)
        .flatMap(
            pr ->
                documentQueryHelper
                    .findAndValidateParty(pr.getContractId(), userId)
                    .flatMap(this::requireActive)
                    .flatMap(
                        doc -> {
                          if (user.user().identifier() != null
                              && user.user().identifier().equals(pr.getRequesterIn())) {
                            return Mono.<ProductRequestEntity>error(
                                new ForbiddenException("So'rovni yaratuvchi o'zi rad eta olmaydi"));
                          }
                          pr.setStatus(ProductRequestStatus.REJECTED);
                          return productRequestRepository.save(pr);
                        }))
        .then();
  }

  // ================ CANCEL (so'rovchi o'z so'rovini bekor qiladi) ================

  @Transactional
  public Mono<Void> cancel(UUID id) {
    return pending(id)
        .flatMap(
            pr ->
                documentQueryHelper
                    .findOrThrow(pr.getContractId())
                    .flatMap(this::requireActive)
                    .flatMap(
                        doc -> {
                          pr.setStatus(ProductRequestStatus.CANCELLED);
                          return productRequestRepository.save(pr);
                        }))
        .then();
  }

  // ================ LIST ================

  public Flux<ProductRequestResponse> getFiltered(
      String buyerIn,
      String sellerIn,
      String fromIn,
      String toIn,
      UUID contractId,
      List<ProductRequestStatus> statuses) {
    return productRequestRepository
        .findFiltered(buyerIn, sellerIn, fromIn, toIn, contractId, statuses)
        .map(productRequestMapper::toResponse);
  }

  // Shartnoma ichiga nest qilish uchun — bitta shartnomaning barcha so'rovlari.
  public Flux<ProductRequestResponse> getByContract(UUID contractId) {
    return productRequestRepository
        .findAllByContractIdAndDeletedFalseOrderByCreatedDateDesc(contractId)
        .map(productRequestMapper::toResponse);
  }

  // ================ HELPERS ================

  // Mahsulot oldi-berdisi (yaratish/tasdiq/rad/bekor) FAQAT shartnoma ACTIVE
  // (ikkala taraf imzolagan) bo'lganda ishlaydi — CREATED/COMPLETED/REJECTED/CANCELLED'da yo'q.
  private Mono<DocumentEntity> requireActive(DocumentEntity doc) {
    if (doc.getStatus() != DocumentStatus.ACTIVE) {
      return Mono.error(
          new InvalidOperationException("Mahsulot oldi-berdisi faqat aktiv shartnomada mumkin"));
    }
    return Mono.just(doc);
  }

  private Mono<ProductRequestEntity> pending(UUID id) {
    return productRequestRepository
        .findByIdAndDeletedFalse(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Mahsulot so'rovi topilmadi")))
        .flatMap(
            pr ->
                pr.getStatus() == ProductRequestStatus.PENDING
                    ? Mono.just(pr)
                    : Mono.error(new InvalidOperationException("So'rov PENDING holatida emas")));
  }
}
