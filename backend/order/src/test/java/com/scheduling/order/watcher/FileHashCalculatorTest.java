package com.scheduling.order.watcher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileHashCalculatorTest {

    private final FileHashCalculator hasher = new FileHashCalculator();

    @Test
    @DisplayName("SHA-256 — 알려진 input ('abc') → e9c0f8b8... 검증")
    void sha256_known_value(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("abc.bin");
        Files.writeString(f, "abc");

        String hash = hasher.sha256(f);

        // NIST SHA-256 표준 — "abc" 의 정답
        assertThat(hash).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(hash).hasSize(64);
    }

    @Test
    @DisplayName("동일 콘텐츠 → 동일 해시 (idempotent)")
    void same_content_same_hash(@TempDir Path tmp) throws IOException {
        Path f1 = tmp.resolve("a.bin");
        Path f2 = tmp.resolve("b.bin");
        Files.writeString(f1, "TK-01-3-2 hash test");
        Files.writeString(f2, "TK-01-3-2 hash test");

        assertThat(hasher.sha256(f1)).isEqualTo(hasher.sha256(f2));
    }

    @Test
    @DisplayName("다른 콘텐츠 → 다른 해시")
    void different_content_different_hash(@TempDir Path tmp) throws IOException {
        Path f1 = tmp.resolve("a.bin");
        Path f2 = tmp.resolve("b.bin");
        Files.writeString(f1, "content-1");
        Files.writeString(f2, "content-2");

        assertThat(hasher.sha256(f1)).isNotEqualTo(hasher.sha256(f2));
    }

    @Test
    @DisplayName("존재하지 않는 파일 → IllegalStateException")
    void missing_file_throws() {
        assertThatThrownBy(() -> hasher.sha256(Path.of("/non-existent-file-tk-01-3-2.bin")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SHA-256 해시 실패");
    }
}
