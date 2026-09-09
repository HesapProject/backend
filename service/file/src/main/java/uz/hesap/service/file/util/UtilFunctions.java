package uz.hesap.service.file.util;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class UtilFunctions {

  public static String getFileExtension(String originalFilename) {
    if (originalFilename == null) {
      log.error("Original filename is null");
      throw new IllegalArgumentException("Original filename is null");
    }

    final String[] split = originalFilename.split("\\.");
    if (split.length == 0) {
      log.error("Can't get file ext");
      throw new IllegalArgumentException("Can't get file ext");
    }

    return split[split.length - 1];
  }
}
