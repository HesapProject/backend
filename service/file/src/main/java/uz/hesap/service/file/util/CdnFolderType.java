package uz.hesap.service.file.util;

import java.util.Set;
import lombok.Getter;

/** Allowed folder types for CDN uploads */
@Getter
public enum CdnFolderType {
  IMAGES("images"),
  STORIES("stories"),
  VIDEOS("videos"),
  CONTRACTS("contracts");

  private final String folderName;

  CdnFolderType(String folderName) {
    this.folderName = folderName;
  }

  private static final Set<String> VALID_FOLDERS =
      Set.of("images", "stories", "videos", "contracts");

  public static boolean isValidFolder(String folder) {
    return folder != null && VALID_FOLDERS.contains(folder.toLowerCase());
  }
}
