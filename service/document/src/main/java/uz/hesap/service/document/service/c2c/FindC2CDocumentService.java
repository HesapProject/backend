package uz.hesap.service.document.service.c2c;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.document.domain.document.ContractProductEntity;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.document.WitnessRequestEntity;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.model.mapper.DocumentMapper;
import uz.hesap.service.document.model.response.C2CDocumentResponse;
import uz.hesap.service.document.model.response.ContractProductResponse;
import uz.hesap.service.document.model.response.PendingDocumentStats;
import uz.hesap.service.document.model.response.WitnessResponse;
import uz.hesap.service.document.repository.*;
import uz.hesap.service.document.service.document.DocumentQueryHelper;
import uz.hesap.service.document.webclient.UserServiceClient;

@Service
@RequiredArgsConstructor
@Log4j2
public class FindC2CDocumentService {
  private final DocumentRepository documentRepository;
  private final CustomDocumentRepository customDocumentRepository;
  private final UserServiceClient userServiceClient;
  private final WitnessRequestRepository witnessRequestRepository;
  private final ContractProductRepository contractProductRepository;
  private final ObjectMapper objectMapper;
  private final DocumentQueryHelper documentQueryHelper;
  private final CancelRequestsService cancelRequestsService;

  public Mono<C2CDocumentResponse> getById(UUID id, UUID userId) {
    return documentQueryHelper
        .findAndValidateParty(id, userId)
        .flatMap(
            doc ->
                Mono.zip(
                        witnessRequestRepository.findAllByContractId(doc.getId()).collectList(),
                        contractProductRepository
                            .findAllByDocumentIdAndDeletedFalse(doc.getId())
                            .collectList(),
                        cancelRequestsService.getByContract(doc.getId()).collectList())
                    .flatMap(
                        tuple -> {
                          List<WitnessRequestEntity> witnesses = tuple.getT1();
                          List<ContractProductEntity> products = tuple.getT2();
                          var cancelRequests = tuple.getT3();
                          return enrich(List.of(doc), witnesses)
                              .map(
                                  ctx ->
                                      mapToC2CResponse(
                                          doc,
                                          ctx.partyUsers(),
                                          ctx.witnessUsers(),
                                          witnesses,
                                          products,
                                          cancelRequests));
                        }));
  }

  public Mono<Page<C2CDocumentResponse>> getAll(UUID userId, Pageable pageable) {
    // Taraflar PINFL'da — joriy user PINFL'ini bir marta resolish qilamiz.
    return userServiceClient
        .getUserById(userId)
        .flatMap(
            me -> {
              String userIn = me.in();
              return customDocumentRepository
                  .findFiltered(userIn, null, null, null, null, null, pageable)
                  .collectList()
                  .flatMap(documents -> pageOf(documents, pageable, countAll(userIn)));
            });
  }

  private Mono<Long> countAll(String userIn) {
    // 'In' keyword kolliziyasidan qochish uchun derived emas, raw-SQL filtr.
    return customDocumentRepository.countFiltered(userIn, null, null, null, null, null);
  }

  private C2CDocumentResponse mapToC2CResponse(
      DocumentEntity doc,
      Map<String, UserResponse> partyUsers,
      Map<UUID, UserResponse> witnessUsers,
      List<WitnessRequestEntity> witnesses,
      List<ContractProductEntity> products,
      List<uz.hesap.service.document.model.response.CancelRequestResponse> cancelRequests) {
    UserResponse buyer = partyUsers.get(doc.getBuyerIn());
    UserResponse seller = partyUsers.get(doc.getSellerIn());

    List<WitnessResponse> witnessResponses =
        witnesses.stream()
            .map(
                w ->
                    new WitnessResponse(
                        witnessUsers.get(w.getWitnessId()),
                        w.getStatus(),
                        w.getLastModifiedDate()))
            .toList();

    List<ContractProductResponse> productResponses =
        products.stream().map(this::toProductResponse).toList();

    Object docContent = null;
    if (doc.getDocumentJson() != null) {
      try {
        docContent = objectMapper.readTree(doc.getDocumentJson());
      } catch (JsonProcessingException e) {
        log.error("Failed to parse document json", e);
        docContent = doc.getDocumentJson(); // Fallback to string
      }
    }

    return DocumentMapper.INSTANCE.toC2CResponse(
        doc, buyer, seller, witnessResponses, productResponses, cancelRequests, docContent);
  }

  // ContractProductEntity → response (field_values JSON → Map).
  private ContractProductResponse toProductResponse(ContractProductEntity e) {
    Map<String, Object> values = null;
    if (e.getFieldValues() != null && !e.getFieldValues().isBlank()) {
      try {
        values =
            objectMapper.readValue(
                e.getFieldValues(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
      } catch (JsonProcessingException ex) {
        log.warn("Contract product values JSON parse failed: {}", ex.getMessage());
      }
    }
    return new ContractProductResponse(
        e.getId(), e.getName(), e.getUnit(), e.getPrice(), e.getQuantity(), e.getAmount(),
        e.getDeliveryAt(), values);
  }

  // pending document statistikasi — taraf identifikatori (in/STIR) bo'yicha
  public Mono<PendingDocumentStats> getPendingStats(String in) {
    return customDocumentRepository.getPendingStats(in);
  }

  // status filter bilan getAll
  public Mono<Page<C2CDocumentResponse>> getAllFiltered(
      UUID userId, List<DocumentStatus> statuses, Pageable pageable) {
    return userServiceClient
        .getUserById(userId)
        .flatMap(
            me -> {
              String userIn = me.in();
              return customDocumentRepository
                  .findFiltered(userIn, statuses, null, null, null, null, pageable)
                  .collectList()
                  .flatMap(
                      documents ->
                          pageOf(
                              documents,
                              pageable,
                              customDocumentRepository.countFiltered(
                                  userIn, statuses, null, null, null, null)));
            });
  }

  // documents ro'yxatini guvoh + taraf userlari bilan boyitib Page'ga yig'adi.
  private Mono<Page<C2CDocumentResponse>> pageOf(
      List<DocumentEntity> documents, Pageable pageable, Mono<Long> countMono) {
    if (documents.isEmpty()) {
      return Mono.just(new PageImpl<>(List.of(), pageable, 0));
    }
    List<UUID> docIds = documents.stream().map(DocumentEntity::getId).toList();
    return witnessRequestRepository
        .findAllByContractIdIn(docIds)
        .collectList()
        .flatMap(
            allWitnesses ->
                enrich(documents, allWitnesses)
                    .flatMap(
                        ctx -> {
                          List<C2CDocumentResponse> responses =
                              documents.stream()
                                  .map(
                                      doc -> {
                                        List<WitnessRequestEntity> docWitnesses =
                                            allWitnesses.stream()
                                                .filter(w -> w.getContractId().equals(doc.getId()))
                                                .toList();
                                        return mapToC2CResponse(
                                            doc,
                                            ctx.partyUsers(),
                                            ctx.witnessUsers(),
                                            docWitnesses,
                                            List.of(),
                                            List.of());
                                      })
                                  .toList();
                          return countMono.map(
                              count -> new PageImpl<>(responses, pageable, count));
                        }));
  }

  // Taraflar (buyer_in/seller_in → UserResponse) + guvohlar (witness_id → UserResponse).
  private Mono<EnrichContext> enrich(
      List<DocumentEntity> documents, List<WitnessRequestEntity> witnesses) {
    List<String> partyIns = new ArrayList<>();
    documents.forEach(
        d -> {
          partyIns.add(d.getBuyerIn());
          partyIns.add(d.getSellerIn());
        });
    List<UUID> witnessIds = witnesses.stream().map(WitnessRequestEntity::getWitnessId).toList();
    return Mono.zip(
        userServiceClient.getUsersByIns(partyIns),
        userServiceClient
            .getUsersByIds(witnessIds)
            .collectMap(UserResponse::id, Function.identity()),
        EnrichContext::new);
  }

  private record EnrichContext(
      Map<String, UserResponse> partyUsers, Map<UUID, UserResponse> witnessUsers) {}
}
