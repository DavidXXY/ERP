package com.company.ops.api.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.storage.FileStorageService.FilePolicy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageServiceTest {
  private static final FilePolicy PDF_ONLY = new FilePolicy(
      1024,
      Set.of(".pdf"),
      "文件过大",
      "仅支持 PDF",
      true
  );

  @TempDir
  Path tempDir;

  @Test
  void storeSanitizesOriginalNameAndKeepsFileInsideNamespace() throws Exception {
    LocalFileStorageService service = new LocalFileStorageService(tempDir.toString());
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "../contract.pdf",
        "application/pdf",
        "hello".getBytes()
    );

    var stored = service.store(file, "office", PDF_ONLY);

    assertThat(stored.originalName()).isEqualTo("contract.pdf");
    assertThat(stored.relativePath()).startsWith("office/");
    assertThat(stored.path()).startsWith(tempDir.resolve("office").toAbsolutePath().normalize());
    assertThat(Files.exists(stored.path())).isTrue();
  }

  @Test
  void storeRejectsUnsupportedExtension() {
    LocalFileStorageService service = new LocalFileStorageService(tempDir.toString());
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "script.sh",
        "text/x-shellscript",
        "echo bad".getBytes()
    );

    assertThatThrownBy(() -> service.store(file, "office", PDF_ONLY))
        .isInstanceOf(BusinessException.class)
        .hasMessage("仅支持 PDF");
  }

  @Test
  void storeRejectsMismatchedStrictContentType() {
    LocalFileStorageService service = new LocalFileStorageService(tempDir.toString());
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "contract.pdf",
        "image/png",
        "not really pdf".getBytes()
    );

    assertThatThrownBy(() -> service.store(file, "office", PDF_ONLY))
        .isInstanceOf(BusinessException.class)
        .hasMessage("文件类型与扩展名不匹配");
  }
}
