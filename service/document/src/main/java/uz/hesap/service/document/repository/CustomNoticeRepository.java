package uz.hesap.service.document.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.document.NoticeEntity;

// Talabnomalar (notices) — fromIn (yuboruvchi PINFL) / toIn (qabul qiluvchi PINFL) bo'yicha filtr.
public interface CustomNoticeRepository {
  Flux<NoticeEntity> findFiltered(String fromIn, String toIn, int size, long offset);

  Mono<Long> countFiltered(String fromIn, String toIn);
}
