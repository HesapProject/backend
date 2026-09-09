package uz.hesap.service.file.service;

import static java.nio.file.attribute.PosixFilePermission.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.file.model.FileResponse;
import uz.hesap.service.file.util.UtilFunctions;

@Log4j2
@Service
@RequiredArgsConstructor
public class FileService {

  private static final Path ROOT = Paths.get("/files"); // <— your FS path

  private static final Set<PosixFilePermission> DIR_PERMS =
      PosixFilePermissions.fromString("rwxr-xr-x"); // 755
  private static final Set<PosixFilePermission> FILE_PERMS =
      Set.of(
          OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ // 0644
          );

  public Mono<FileResponse> upload(UserPrincipal user, Mono<FilePart> filePartMono) {
    return filePartMono
        .doOnNext(fp -> log.debug("Received file {}", fp.filename()))
        .flatMap(
            fp -> {
              final UUID userId = user.user().id();
              final String ext = UtilFunctions.getFileExtension(fp.filename()); // keep your util
              final String filename = UUID.randomUUID() + (ext.isBlank() ? "" : "." + ext);
              final Path userDir = ROOT.resolve(userId.toString()).normalize();
              final Path dest = userDir.resolve(filename).normalize();

              if (!dest.startsWith(ROOT)) {
                return Mono.error(new SecurityException("Invalid path"));
              }

              Mono<Void> ensureDir =
                  Mono.fromRunnable(() -> createDirIfNeeded(userDir))
                      .subscribeOn(Schedulers.boundedElastic())
                      .then();

              Mono<Void> save = fp.transferTo(dest);

              Mono<Void> perms =
                  Mono.fromRunnable(() -> setFilePerms(dest))
                      .subscribeOn(Schedulers.boundedElastic())
                      .then();

              return ensureDir
                  .then(save)
                  .then(perms)
                  .thenReturn(new FileResponse("/files/" + userId + "/" + filename));
            });
  }

  public Mono<Boolean> delete(final String urlPath) {
    return Mono.fromCallable(
            () -> {
              Path dest = ROOT.resolve(stripFilesPrefix(urlPath)).normalize();
              if (!dest.startsWith(ROOT)) throw new SecurityException("Invalid path");
              try {
                Files.deleteIfExists(dest);
                return Boolean.TRUE;
              } catch (IOException e) {
                log.error("Delete failed: {}", e.getMessage());
                return Boolean.FALSE;
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  // ===== helpers =====

  private static String stripFilesPrefix(String urlPath) {
    String p = urlPath.startsWith("/files/") ? urlPath.substring(7) : urlPath;
    if (p.startsWith("/")) p = p.substring(1);
    return p;
  }

  private static void createDirIfNeeded(Path dir) {
    try {
      if (Files.notExists(dir)) {
        Files.createDirectories(dir, PosixFilePermissions.asFileAttribute(DIR_PERMS)); // 755
      } else {
        // make sure execute bit exists so nginx can traverse
        setDirPerms(dir);
      }
      // optional: if you rely on group inheritance, you can set setgid bit here:
      // Files.setAttribute(dir, "unix:mode", 02775); // needs care; skip if unsure
    } catch (IOException e) {
      throw new RuntimeException("Failed to create directory " + dir + ": " + e.getMessage(), e);
    }
  }

  private static void setDirPerms(Path dir) {
    try {
      Files.setPosixFilePermissions(dir, DIR_PERMS); // 755
    } catch (IOException e) {
      // non-fatal
    }
  }

  private static void setFilePerms(Path file) {
    try {
      Files.setPosixFilePermissions(file, FILE_PERMS); // 0644
      // Optional: set group to www-data if you need:
      // GroupPrincipal www = file.getFileSystem().getUserPrincipalLookupService()
      //     .lookupPrincipalByGroupName("www-data");
      // Files.getFileAttributeView(file, PosixFileAttributeView.class).setGroup(www);
    } catch (IOException e) {
      throw new RuntimeException("Failed to set perms on " + file + ": " + e.getMessage(), e);
    }
  }
}
