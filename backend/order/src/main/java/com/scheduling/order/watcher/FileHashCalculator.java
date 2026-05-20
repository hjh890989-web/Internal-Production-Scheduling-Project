package com.scheduling.order.watcher;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 파일 해시 — TK-01-3-2 중복 검출 키.
 *
 * <p>{@link MessageDigest} thread-safe X — 매 호출 새 인스턴스. 8KB 버퍼.
 */
@Component
public class FileHashCalculator {

    private static final int BUF_SIZE = 8192;

    /**
     * SHA-256 hex (64 chars) 반환.
     *
     * @throws IllegalStateException IO 또는 알고리즘 실패 시
     */
    public String sha256(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[BUF_SIZE];
            int read;
            while ((read = is.read(buf)) > 0) {
                md.update(buf, 0, read);
            }
            return toHex(md.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 해시 실패: " + file, e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
