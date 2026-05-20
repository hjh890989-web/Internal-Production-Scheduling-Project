package com.scheduling.order.watcher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathMultipartFileTest {

    @Test
    @DisplayName("metadata — filename + size + content-type 정확")
    void metadata_correct(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("sample.xlsx");
        byte[] payload = "PK fake xlsx".getBytes();
        Files.write(file, payload);

        PathMultipartFile mf = new PathMultipartFile(file);

        assertThat(mf.getOriginalFilename()).isEqualTo("sample.xlsx");
        assertThat(mf.getName()).isEqualTo("files");
        assertThat(mf.getSize()).isEqualTo(payload.length);
        assertThat(mf.getContentType()).contains("openxmlformats");
        assertThat(mf.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("getBytes — 파일 내용과 동일")
    void get_bytes_matches_file(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("sample.xlsx");
        byte[] payload = new byte[]{1, 2, 3, 4, 5};
        Files.write(file, payload);

        PathMultipartFile mf = new PathMultipartFile(file);

        assertThat(mf.getBytes()).containsExactly(payload);
    }

    @Test
    @DisplayName("getInputStream — 스트림 읽기")
    void get_input_stream(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("sample.xlsx");
        Files.writeString(file, "stream-test");

        PathMultipartFile mf = new PathMultipartFile(file);

        try (InputStream is = mf.getInputStream()) {
            assertThat(new String(is.readAllBytes())).isEqualTo("stream-test");
        }
    }

    @Test
    @DisplayName("0 byte 파일 → isEmpty true")
    void empty_file(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("empty.xlsx");
        Files.createFile(file);

        PathMultipartFile mf = new PathMultipartFile(file);

        assertThat(mf.isEmpty()).isTrue();
        assertThat(mf.getSize()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 파일 → 생성자에서 IOException")
    void missing_file_throws() {
        assertThatThrownBy(() -> new PathMultipartFile(Path.of("/non-existent-tk-01-3-2.xlsx")))
            .isInstanceOf(IOException.class);
    }
}
