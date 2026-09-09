package uz.hesap.service.main.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.StoryEntity;
import uz.hesap.service.main.model.StoryRequest;
import uz.hesap.service.main.model.StoryResponse;
import uz.hesap.service.main.model.mapper.StoryMapper;
import uz.hesap.service.main.repository.StoryRepository;
import uz.hesap.service.main.repository.StoryViewRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class StoryService {

  private final StoryRepository storyRepository;
  private final StoryViewRepository storyViewRepository;

  public Mono<StoryResponse> createStory(StoryRequest request) {
    log.info("Creating new story");
    StoryEntity entity = StoryMapper.INSTANCE.toEntity(request);
    return storyRepository.save(entity).map(StoryMapper.INSTANCE::toResponse);
  }

  public Mono<StoryResponse> updateStory(UUID id, StoryRequest request) {
    log.info("Updating story with id: {}", id);
    return storyRepository
        .findByIdAndIsDeletedFalse(id)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Story not found: " + id)))
        .flatMap(
            existing -> {
              StoryMapper.INSTANCE.updateEntity(request, existing);
              return storyRepository.save(existing);
            })
        .map(StoryMapper.INSTANCE::toResponse);
  }

  public Mono<Void> deleteStory(UUID id) {
    log.info("Deleting story with id: {}", id);
    return storyRepository
        .findByIdAndIsDeletedFalse(id)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Story not found: " + id)))
        .flatMap(
            entity -> {
              entity.setIsDeleted(Boolean.TRUE);
              return storyRepository.save(entity);
            })
        .then();
  }

  public Mono<StoryResponse> getStoryById(UUID id) {
    log.debug("Fetching story by id: {}", id);
    return storyRepository.findByIdAndIsDeletedFalse(id).map(StoryMapper.INSTANCE::toResponse);
  }

  public Mono<List<StoryResponse>> findAll() {
    log.debug("Fetching all stories");
    return storyRepository
        .findAllByIsDeletedFalseOrderByCreatedDateDesc()
        .map(StoryMapper.INSTANCE::toResponse)
        .collectList();
  }

  public Mono<List<StoryResponse>> getAllStories(UUID userId) {
    log.debug("Fetching all stories for userId: {}", userId);
    return storyRepository
        .findAllWithViewStatus(userId)
        .map(StoryMapper.INSTANCE::projectionToResponse)
        .collectList();
  }

  public Mono<Void> markAsViewed(UUID storyId, UUID userId) {
    log.debug("Marking story {} as viewed by user {}", storyId, userId);
    return storyViewRepository.insertIfNotExists(storyId, userId);
  }
}
