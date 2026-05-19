package com.scheduling.order.import_;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Excel import 비동기 처리 전용 TaskExecutor — TK-01-1-3.
 *
 * <p>사용자 ~10명·동시 import 가정 ≤4 → core=4, max=8, queue=50.
 * CallerRunsPolicy: 큐 풀 시 호출 스레드에서 실행 (요청 거부 회피, 응답 시간 ↑ 신호).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String EXCEL_IMPORT_EXECUTOR = "excelImportExecutor";

    @Bean(EXCEL_IMPORT_EXECUTOR)
    public Executor excelImportExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(4);
        exec.setMaxPoolSize(8);
        exec.setQueueCapacity(50);
        exec.setThreadNamePrefix("excel-import-");
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.initialize();
        return exec;
    }
}
