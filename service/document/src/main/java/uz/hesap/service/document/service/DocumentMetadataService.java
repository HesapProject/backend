package uz.hesap.service.document.service;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.model.response.IdExtractionResult;
import uz.hesap.service.document.model.response.MetadataBundle;

/**
 * Umumiy metadata batch fetch service. Taraflar va yaratuvchi endi PINFL/STIR bilan
 * identifikatsiya qilinadi va ContractsService'da enrich qilinadi — bu yerda UUID id yo'q.
 */
@Service
@RequiredArgsConstructor
public class DocumentMetadataService {

  /** UUID id'lar bo'yicha enrichment endi yo'q (taraflar/creator PINFL) — bo'sh bundle. */
  public Mono<MetadataBundle> fetchMetadata(IdExtractionResult ids) {
    return Mono.just(new MetadataBundle(Map.of(), Map.of()));
  }

  /** Document ro'yxatidan UUID id'lar — taraflar endi PINFL (buyer_in/seller_in), bu yerda bo'sh. */
  public IdExtractionResult extractFromDocuments(List<DocumentEntity> entities) {
    return new IdExtractionResult(Set.of());
  }
}
