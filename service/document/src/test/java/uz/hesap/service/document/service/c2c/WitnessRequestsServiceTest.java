package uz.hesap.service.document.service.c2c;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.common.util.message.NotificationEvent;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.document.WitnessRequestEntity;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.repository.DocumentRepository;
import uz.hesap.service.document.repository.WitnessRequestRepository;
import uz.hesap.service.document.service.template.TemplateNotificationDispatcher;
import uz.hesap.service.document.webclient.UserServiceClient;

/** addWitness — shartnoma tarafi (buyer/seller) guvoh bo'la olmasligi guard'i. */
@ExtendWith(MockitoExtension.class)
class WitnessRequestsServiceTest {

  private static final String BUYER_IN = "11111111111";
  private static final String SELLER_IN = "22222222222";
  private static final String THIRD_PARTY_IN = "33333333333";

  @Mock private WitnessRequestRepository witnessRequestRepository;
  @Mock private UserServiceClient userServiceClient;
  @Mock private DocumentRepository documentRepository;
  @Mock private TemplateNotificationDispatcher notificationDispatcher;

  @InjectMocks private WitnessRequestsService service;

  private DocumentEntity editableContract() {
    DocumentEntity doc = new DocumentEntity();
    doc.setId(UUID.randomUUID());
    doc.setStatus(DocumentStatus.CREATED);
    doc.setBuyerIn(BUYER_IN);
    doc.setSellerIn(SELLER_IN);
    doc.setTemplateId(UUID.randomUUID());
    doc.setNumber("DOC-1");
    return doc;
  }

  private UserResponse userWithIn(UUID id, String in) {
    return UserResponse.builder().id(id).in(in).build();
  }

  @Test
  void addWitness_rejectsBuyerAsWitness() {
    DocumentEntity doc = editableContract();
    UUID witnessId = UUID.randomUUID();
    when(documentRepository.findByIdAndDeletedFalse(doc.getId())).thenReturn(Mono.just(doc));
    when(witnessRequestRepository.findByContractIdAndWitnessId(doc.getId(), witnessId))
        .thenReturn(Mono.empty());
    when(userServiceClient.getUserById(witnessId))
        .thenReturn(Mono.just(userWithIn(witnessId, BUYER_IN)));

    StepVerifier.create(service.addWitness(doc.getId(), witnessId, BUYER_IN))
        .expectErrorMatches(
            e ->
                e instanceof BadRequestException
                    && e.getMessage().equals("Shartnoma tarafi guvoh bo'la olmaydi"))
        .verify();

    verify(witnessRequestRepository, never()).save(any());
  }

  @Test
  void addWitness_rejectsSellerAsWitness() {
    DocumentEntity doc = editableContract();
    UUID witnessId = UUID.randomUUID();
    when(documentRepository.findByIdAndDeletedFalse(doc.getId())).thenReturn(Mono.just(doc));
    when(witnessRequestRepository.findByContractIdAndWitnessId(doc.getId(), witnessId))
        .thenReturn(Mono.empty());
    when(userServiceClient.getUserById(witnessId))
        .thenReturn(Mono.just(userWithIn(witnessId, SELLER_IN)));

    StepVerifier.create(service.addWitness(doc.getId(), witnessId, BUYER_IN))
        .expectError(BadRequestException.class)
        .verify();

    verify(witnessRequestRepository, never()).save(any());
  }

  @Test
  void addWitness_acceptsThirdParty() {
    DocumentEntity doc = editableContract();
    UUID witnessId = UUID.randomUUID();
    when(documentRepository.findByIdAndDeletedFalse(doc.getId())).thenReturn(Mono.just(doc));
    when(witnessRequestRepository.findByContractIdAndWitnessId(doc.getId(), witnessId))
        .thenReturn(Mono.empty());
    when(userServiceClient.getUserById(witnessId))
        .thenReturn(Mono.just(userWithIn(witnessId, THIRD_PARTY_IN)));
    when(witnessRequestRepository.save(any(WitnessRequestEntity.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    when(notificationDispatcher.dispatch(
            any(), eq(NotificationEvent.WITNESS), eq(witnessId), anyString(), any(), any()))
        .thenReturn(Mono.empty());

    StepVerifier.create(service.addWitness(doc.getId(), witnessId, BUYER_IN)).verifyComplete();

    verify(witnessRequestRepository).save(any(WitnessRequestEntity.class));
  }
}
