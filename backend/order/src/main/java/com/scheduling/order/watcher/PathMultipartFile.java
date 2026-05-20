package com.scheduling.order.watcher;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@link Path} → {@link MultipartFile} 어댑터 — TK-01-3-2.
 *
 * <p>folder watcher 가 발견한 파일을 기존 {@code ImportOrchestratorService.processAsync}
 * (List&lt;MultipartFile&gt; 시그니처) 에 그대로 전달하기 위한 경량 wrapper.
 * 메모리 적재 X — getBytes() 호출 시점에만 읽음.
 */
public class PathMultipartFile implements MultipartFile {

    private static final String CONTENT_TYPE_XLSX =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final Path path;
    private final String name;
    private final long size;

    public PathMultipartFile(Path path) throws IOException {
        this.path = path;
        this.name = path.getFileName().toString();
        this.size = Files.size(path);
    }

    @Override
    public String getName() {
        return "files";
    }

    @Override
    public String getOriginalFilename() {
        return name;
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE_XLSX;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(path);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public void transferTo(File dest) throws IOException {
        Files.copy(path, dest.toPath());
    }
}
