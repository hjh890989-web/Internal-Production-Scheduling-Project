-- =============================================================================
-- V031__create_event_publication.sql — TK-41-2-2 (EP-41 ST-41-2)
-- =============================================================================
-- spring-modulith-events-jpa event_publication 테이블 — Modulith @ApplicationModuleListener
-- 가 영속 publication 기록. 재시작 시 미완료 publication 복구 (BR-X03 cascade 안정성).
--
-- schema: app (default Flyway schema, app 우선). spring-modulith 1.4.x PostgreSQL DDL 정합.
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.event_publication (
    id                  UUID                       NOT NULL,
    listener_id         VARCHAR(512)               NOT NULL,
    event_type          VARCHAR(512)               NOT NULL,
    serialized_event    TEXT                       NOT NULL,
    publication_date    TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    completion_date     TIMESTAMP(9) WITH TIME ZONE,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx
    ON public.event_publication USING hash (serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx
    ON public.event_publication (completion_date);

COMMENT ON TABLE public.event_publication IS
    'EP-41 ST-41-2 spring-modulith-events-jpa 영속 publication (재시작 복구)';
