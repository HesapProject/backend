package uz.hesap.service.main.api.v1;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.main.model.StoryRequest;
import uz.hesap.service.main.model.StoryResponse;
import uz.hesap.service.main.model.StoryViewRequest;
import uz.hesap.service.main.service.StoryService;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/main/v1/stories")
public class StoryController {

  private final StoryService storyService;

  @PostMapping
  public Mono<StoryResponse> createStory(@RequestBody StoryRequest request) {
    return storyService.createStory(request);
  }

  @PutMapping("/{id}")
  public Mono<StoryResponse> updateStory(@PathVariable UUID id, @RequestBody StoryRequest request) {
    return storyService.updateStory(id, request);
  }

  @DeleteMapping("/{id}")
  public Mono<Void> deleteStory(@PathVariable UUID id) {
    return storyService.deleteStory(id);
  }

  @GetMapping("/{id}")
  public Mono<StoryResponse> getStoryById(@PathVariable UUID id) {
    return storyService.getStoryById(id);
  }

  @GetMapping
  public Mono<List<StoryResponse>> getAllStories() {
    return storyService.findAll();
  }

  @GetMapping("/user")
  public Mono<List<StoryResponse>> findAll(@AuthenticationPrincipal UserPrincipal userPrincipal) {
    return storyService.getAllStories(userPrincipal.user().id());
  }

  @PostMapping("/view")
  public Mono<Void> markAsViewed(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody StoryViewRequest request) {
    //        return storyService.markAsViewed(request.storyId(), userPrincipal.user().id());
    return storyService.markAsViewed(
        request.storyId(), UUID.fromString("0038f6c5-ec93-4647-aaaa-1bebf17cfc38"));
  }
}
