-- PostgreSQL 16+, UTF-8. Run with psql -v ON_ERROR_STOP=1 -f 001_stage2.sql
-- No vector extension is needed until stage 3. Re-running is safe; this is NOT an upgrade script.
BEGIN;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE TABLE IF NOT EXISTS atlas_document (
  id uuid PRIMARY KEY,
  source_key text NOT NULL UNIQUE,
  source_type text NOT NULL CHECK (source_type IN ('WEB_PAGE','FILE','MANUAL_NOTE')),
  source_name text NOT NULL DEFAULT '未知',
  url text,
  archived boolean NOT NULL DEFAULT false,
  current_version_id uuid,
  searchable_version_id uuid,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS atlas_document_version (
  id uuid PRIMARY KEY,
  document_id uuid NOT NULL REFERENCES atlas_document(id) ON DELETE CASCADE,
  sequence_no integer NOT NULL,
  content_hash text NOT NULL,
  raw_hash text NOT NULL,
  raw_content bytea NOT NULL,
  content text NOT NULL,
  parser_version text NOT NULL,
  title text NOT NULL DEFAULT '未知',
  technology text NOT NULL DEFAULT '未知',
  version_range text NOT NULL DEFAULT '未知',
  language text NOT NULL DEFAULT '未知',
  tags text[] NOT NULL DEFAULT '{}',
  author text NOT NULL DEFAULT '未知',
  license text NOT NULL DEFAULT '未知',
  trust text NOT NULL DEFAULT '未知',
  review_status text NOT NULL DEFAULT 'PENDING_REVIEW' CHECK (review_status IN ('PENDING_REVIEW','APPROVED','REJECTED')),
  index_status text NOT NULL DEFAULT 'NOT_INDEXED' CHECK (index_status IN ('NOT_INDEXED','INDEXED','FAILED')),
  published_at timestamptz,
  source_updated_at timestamptz,
  collected_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(document_id, sequence_no),
  UNIQUE(document_id, content_hash),
  UNIQUE(document_id, id)
);
-- Composite FKs ensure pointers cannot refer to another document's version.
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='atlas_current_version_fk') THEN
    ALTER TABLE atlas_document ADD CONSTRAINT atlas_current_version_fk FOREIGN KEY(id,current_version_id)
      REFERENCES atlas_document_version(document_id,id) DEFERRABLE INITIALLY DEFERRED;
    ALTER TABLE atlas_document ADD CONSTRAINT atlas_searchable_version_fk FOREIGN KEY(id,searchable_version_id)
      REFERENCES atlas_document_version(document_id,id) DEFERRABLE INITIALLY DEFERRED;
  END IF;
END $$;
CREATE TABLE IF NOT EXISTS atlas_chunk (
  id uuid PRIMARY KEY,
  version_id uuid NOT NULL REFERENCES atlas_document_version(id) ON DELETE CASCADE,
  ordinal integer NOT NULL,
  section_path text NOT NULL,
  content text NOT NULL,
  chunker_version text NOT NULL,
  UNIQUE(version_id,ordinal)
);
CREATE INDEX IF NOT EXISTS atlas_chunk_keyword_idx ON atlas_chunk USING gin(content gin_trgm_ops);
CREATE TABLE IF NOT EXISTS atlas_review (
  id uuid PRIMARY KEY,
  version_id uuid NOT NULL REFERENCES atlas_document_version(id) ON DELETE CASCADE,
  decision text NOT NULL CHECK (decision IN ('APPROVED','REJECTED')),
  note text NOT NULL DEFAULT '',
  reviewed_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS atlas_pending_idx ON atlas_document_version(review_status);
COMMIT;
