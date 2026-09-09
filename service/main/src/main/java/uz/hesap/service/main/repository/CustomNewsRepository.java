package uz.hesap.service.main.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.NewsEntity;

public interface CustomNewsRepository {

  Mono<Page<NewsEntity>> findNews(String search, Boolean home, Pageable pageable);
}
