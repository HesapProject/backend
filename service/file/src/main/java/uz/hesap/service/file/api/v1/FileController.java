package uz.hesap.service.file.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.file.model.FileResponse;
import uz.hesap.service.file.service.FileService;

@Log4j2
@RestController
@RequestMapping("/files/v1/upload")
@RequiredArgsConstructor
public class FileController {
  private final FileService fileService;

  @PostMapping
  public Mono<FileResponse> upload(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestPart("file") Mono<FilePart> filePartMono) {
    return fileService.upload(userPrincipal, filePartMono);
  }

  @DeleteMapping
  public Mono<Boolean> delete(@RequestParam String file) {
    return fileService.delete(file);
  }
}
