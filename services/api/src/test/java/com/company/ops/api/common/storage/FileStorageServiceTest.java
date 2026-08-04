package com.company.ops.api.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.storage.FileStorageService.FilePolicy;
import java.io.ByteArrayOutputStream;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {
  private static final FilePolicy POLICY = new FilePolicy(
      1024 * 1024, Set.of(".jpg", ".png", ".pdf", ".docx", ".xlsx"),
      "too large", "unsupported", true);

  @Test
  void rejectsExecutableRenamedAsPdfEvenWithGenericMimeType() {
    var file = new MockMultipartFile("file", "invoice.pdf", "application/octet-stream", "MZ-fake".getBytes());

    assertThatThrownBy(() -> FileStorageService.validate(file, POLICY))
        .isInstanceOf(BusinessException.class)
        .hasMessage("文件内容与扩展名不匹配");
  }

  @Test
  void rejectsImageMimeTypeThatDoesNotMatchExtension() {
    var file = new MockMultipartFile("file", "photo.jpg", "image/png",
        new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

    assertThatThrownBy(() -> FileStorageService.validate(file, POLICY))
        .isInstanceOf(BusinessException.class)
        .hasMessage("文件类型与扩展名不匹配");
  }

  @Test
  void acceptsValidPdfSignatureWithGenericMimeType() {
    var file = new MockMultipartFile("file", "invoice.pdf", "application/octet-stream", "%PDF-1.7\n".getBytes());

    assertThat(FileStorageService.validate(file, POLICY).extension()).isEqualTo(".pdf");
  }

  @Test
  void distinguishesWordAndExcelOpenXmlContainers() throws Exception {
    var word = new MockMultipartFile("file", "report.docx", "application/octet-stream", openXml("word/document.xml"));
    var disguisedExcel = new MockMultipartFile("file", "report.docx", "application/octet-stream", openXml("xl/workbook.xml"));

    assertThat(FileStorageService.validate(word, POLICY).extension()).isEqualTo(".docx");
    assertThatThrownBy(() -> FileStorageService.validate(disguisedExcel, POLICY))
        .isInstanceOf(BusinessException.class)
        .hasMessage("文件内容与扩展名不匹配");
  }

  private byte[] openXml(String documentEntry) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(output)) {
      zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
      zip.write("<Types/>".getBytes());
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry(documentEntry));
      zip.write("content".getBytes());
      zip.closeEntry();
    }
    return output.toByteArray();
  }
}
