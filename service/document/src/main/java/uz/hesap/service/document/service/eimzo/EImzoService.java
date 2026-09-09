package uz.hesap.service.document.service.eimzo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.InvalidOperationException;
import uz.hesap.service.common.exception.handler.EImzoError;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.enums.DocumentPartyStatus;
import uz.hesap.service.document.domain.document.DocumentSignatureEntity;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.SignatureActionType;
import uz.hesap.service.document.model.response.eimzo.EImzoDocBase64;
import uz.hesap.service.document.model.response.eimzo.EImzoVerifyResponse;
import uz.hesap.service.document.repository.DocumentRepository;
import uz.hesap.service.document.repository.DocumentSignatureRepository;
import uz.hesap.service.document.repository.PaymentScheduleRepository;
import uz.hesap.service.document.webclient.IntegrationServiceClient;
import uz.hesap.service.document.webclient.UserServiceClient;

// E-IMZO bilan barcha HTTP integration servisida. document-service faqat
// hujjat orchestratori — sertifikat verifikatsiyasini Feign'dan oladi.
@Log4j2
@Service
@RequiredArgsConstructor
public class EImzoService {
  private final IntegrationServiceClient integrationClient;
  private final DocumentRepository documentRepository;
  private final DocumentSignatureRepository documentSignatureRepository;
  private final PaymentScheduleRepository paymentScheduleRepository;
  private final UserServiceClient userServiceClient;
  private final ObjectMapper objectMapper;

  public Mono<String> getTimestamp(String pkcs7, ServerHttpRequest request) {
    String ipAddress = extractIp(request);
    String host = resolveHost(request);
    return integrationClient.getTimestamp(pkcs7, ipAddress, host);
  }

  @Transactional
  public Mono<Void> signDocument(String pkcs7, ServerHttpRequest request) {
    String ipAddress = extractIp(request);
    String host = resolveHost(request);

    return integrationClient
        .verifyAttached(pkcs7, ipAddress, host)
        .flatMap(
            response -> {
              if (response.status() != 1) {
                String messageByCode = EImzoError.getMessageByCode(response.status());
                return Mono.error(
                    new IllegalArgumentException(messageByCode + " \n " + response.message()));
              }

              Map<String, String> signer =
                  response.pkcs7Info().signers().getFirst().certificate().getFirst().subjectInfo();
              String in = signer.get("1.2.860.3.16.1.2");
              String inn = signer.get("1.2.860.3.16.1.1");
              String documentBase64 = response.pkcs7Info().documentBase64();
              EImzoDocBase64 document = decodeDoc(documentBase64);

              return documentRepository
                  .findById(document.id())
                  .switchIfEmpty(
                      Mono.error(
                          new IllegalArgumentException("Document not found: " + document.id())))
                  .flatMap(
                      doc -> {
                        if (inn != null) {
                          return handleLegalEntitySign(doc, inn, response);
                        } else if (in != null) {
                          return handleNaturalPersonSign(doc, in, response);
                        } else {
                          return Mono.error(
                              new IllegalArgumentException("Signer must have PINFL or INN"));
                        }
                      })
                  .then();
            });
  }

  private Mono<Void> handleNaturalPersonSign(
      DocumentEntity doc, String in, EImzoVerifyResponse response) {
    return userServiceClient
        .getUserByIn(in)
        .switchIfEmpty(
            Mono.error(new IllegalArgumentException("User not found with in: " + in)))
        .flatMap(
            user -> {
              // Taraflar PINFL'da — joriy user PINFL'ini buyer_in/seller_in bilan solishtiramiz.
              SignatureActionType actionType;
              String myPinfl = user.in();
              if (myPinfl != null && myPinfl.equals(doc.getBuyerIn())) {
                actionType = SignatureActionType.SIGN_BY_BUYER;
                doc.setBuyerStatus(DocumentPartyStatus.ACCEPTED);
              } else if (myPinfl != null && myPinfl.equals(doc.getSellerIn())) {
                actionType = SignatureActionType.SIGN_BY_SELLER;
                doc.setSellerStatus(DocumentPartyStatus.ACCEPTED);
              } else {
                return Mono.error(
                    new IllegalArgumentException("User is not a party to this document"));
              }
              // Faqat ikkala tomon imzolasa -> ACTIVE; aks holda CREATED (pending).
              activateIfBothSigned(doc);

              return saveSignature(doc, user.id(), response, actionType)
                  .then(documentRepository.save(doc))
                  .then();
            });
  }

  private Mono<Void> handleLegalEntitySign(
      DocumentEntity doc, String inn, EImzoVerifyResponse response) {
    // Yangi model: yuridik taraf = type=COMPANY user; hujjatning buyer/sellerUserId
    // to'g'ridan-to'g'ri shu company user id (legacy "user".company jadvali emas).
    // Signer cert TIN (inn) hujjatning qaysi tarafiga tegishli ekanini taraf
    // user'larining tin'i bo'yicha topamiz (INN bo'yicha global qidiruv emas —
    // bir xil INN'li dublikat company user'lar bo'lishi mumkin).
    return Mono.zip(fetchPartyUserByIn(doc.getBuyerIn()), fetchPartyUserByIn(doc.getSellerIn()))
        .flatMap(
            tuple -> {
              UserResponse buyer = tuple.getT1().orElse(null);
              UserResponse seller = tuple.getT2().orElse(null);

              SignatureActionType actionType;
              UUID signerUserId;
              if (buyer != null && inn.equals(buyer.tin())) {
                actionType = SignatureActionType.SIGN_BY_BUYER;
                signerUserId = buyer.id();
                doc.setBuyerStatus(DocumentPartyStatus.ACCEPTED);
              } else if (seller != null && inn.equals(seller.tin())) {
                actionType = SignatureActionType.SIGN_BY_SELLER;
                signerUserId = seller.id();
                doc.setSellerStatus(DocumentPartyStatus.ACCEPTED);
              } else {
                return Mono.error(
                    new IllegalArgumentException(
                        "Company (inn " + inn + ") is not a party to this document"));
              }
              // Faqat ikkala tomon imzolasa -> ACTIVE; aks holda CREATED (pending).
              activateIfBothSigned(doc);

              return saveSignature(doc, signerUserId, response, actionType)
                  .then(documentRepository.save(doc))
                  // Shartnoma holati o'zgargan bo'lsa — to'lov jadvallaridagi snapshot'ni ham yangilaymiz.
                  .then(
                      paymentScheduleRepository.updateContractStatusByContractId(
                          doc.getId(), doc.getStatus().name()))
                  .then();
            });
  }

  // Taraf user'ini id bo'yicha oladi; id null yoki topilmasa empty (xato emas) —
  // ikkala tarafni parallel olib, tin bo'yicha moslash uchun.
  private Mono<Optional<UserResponse>> fetchPartyUserByIn(String in) {
    if (in == null || in.isBlank()) {
      return Mono.just(Optional.empty());
    }
    return userServiceClient
        .getUserByIn(in)
        .map(Optional::of)
        .onErrorReturn(Optional.empty());
  }

  // Ikkala taraf ACCEPTED bo'lsa hujjatni ACTIVE qiladi (aks holda CREATED'da qoladi).
  private void activateIfBothSigned(DocumentEntity doc) {
    if (DocumentPartyStatus.ACCEPTED == doc.getBuyerStatus()
        && DocumentPartyStatus.ACCEPTED == doc.getSellerStatus()) {
      doc.setStatus(DocumentStatus.ACTIVE);
    }
  }

  private Mono<DocumentSignatureEntity> saveSignature(
      DocumentEntity doc,
      UUID userId,
      EImzoVerifyResponse response,
      SignatureActionType actionType) {
    DocumentSignatureEntity signatureEntity = new DocumentSignatureEntity();
    signatureEntity.setDocumentId(doc.getId());
    signatureEntity.setUserId(userId);
    signatureEntity.setActionType(actionType);
    signatureEntity.setCreatedDate(Instant.now());
    try {
      signatureEntity.setSignature(objectMapper.writeValueAsString(response.pkcs7Info()));
    } catch (JsonProcessingException e) {
      return Mono.error(new InvalidOperationException("Failed to serialize signature", e));
    }
    return documentSignatureRepository.save(signatureEntity);
  }

  private EImzoDocBase64 decodeDoc(String documentBase64) {
    try {
      Base64.Decoder decoder = Base64.getDecoder();
      byte[] json = decoder.decode(decoder.decode(documentBase64));

      return objectMapper.readValue(json, EImzoDocBase64.class);
    } catch (Exception e) {
      throw new InvalidOperationException("Invalid base64", e);
    }
  }

  // Asl kod `host = "localhost";` qilib over-write qilardi (e-imzo backend localhost'da).
  // Integration servisi DB'dagi base-url'ga yo'naltiradi — Host header esa kelgan
  // hujjat domainini ifodalaydi. Yo'q bo'lsa default "hesap.uz".
  private String resolveHost(ServerHttpRequest request) {
    String host = request.getHeaders().getFirst("Host");
    return host == null || host.isBlank() ? "hesap.uz" : host;
  }

  private String extractIp(ServerHttpRequest request) {
    String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeaders().getFirst("X-Real-IP");
    if (xRealIp != null && !xRealIp.isBlank()) {
      return xRealIp;
    }

    String ip =
        Optional.ofNullable(request.getRemoteAddress())
            .map(InetSocketAddress::getAddress)
            .map(InetAddress::getHostAddress)
            .orElse("127.0.0.1");

    if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
      return "127.0.0.1";
    }

    return ip;
  }
}
