-- Install pgvector on the PostgreSQL server BEFORE executing this file.
-- If unavailable, the whole transaction rolls back; keyword search still works.
BEGIN;
CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE IF NOT EXISTS atlas_embedding_profile (
  id text PRIMARY KEY,
  base_url text NOT NULL,
  model text NOT NULL,
  revision text NOT NULL,
  dimensions integer NOT NULL CHECK(dimensions BETWEEN 1 AND 16000),
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(id,dimensions)
);
CREATE TABLE IF NOT EXISTS atlas_vector_index (
  version_id uuid NOT NULL REFERENCES atlas_document_version(id) ON DELETE CASCADE,
  profile_id text NOT NULL REFERENCES atlas_embedding_profile(id),
  status text NOT NULL CHECK(status IN ('INDEXING','INDEXED','FAILED','STALE')),
  chunk_count integer NOT NULL DEFAULT 0,
  error text,
  started_at timestamptz NOT NULL DEFAULT now(),
  completed_at timestamptz,
  PRIMARY KEY(version_id,profile_id)
);
CREATE TABLE IF NOT EXISTS atlas_chunk_embedding (
  chunk_id uuid NOT NULL REFERENCES atlas_chunk(id) ON DELETE CASCADE,
  profile_id text NOT NULL,
  dimensions integer NOT NULL,
  embedding vector NOT NULL,
  PRIMARY KEY(chunk_id,profile_id),
  FOREIGN KEY(profile_id,dimensions) REFERENCES atlas_embedding_profile(id,dimensions),
  CHECK(vector_dims(embedding)=dimensions),
  CHECK(vector_norm(embedding)>0)
);
-- ponytail: exact cosine scan, no approximate index; add per-profile HNSW after measured growth.
CREATE OR REPLACE FUNCTION atlas_invalidate_vector_index() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  UPDATE atlas_vector_index SET status='STALE',error='正文分块已变化，请重新生成向量'
    WHERE version_id=CASE WHEN TG_OP='DELETE' THEN OLD.version_id ELSE NEW.version_id END;
  RETURN NULL;
END $$;
DROP TRIGGER IF EXISTS atlas_chunk_vector_invalidation ON atlas_chunk;
CREATE TRIGGER atlas_chunk_vector_invalidation AFTER INSERT OR UPDATE OR DELETE ON atlas_chunk
  FOR EACH ROW EXECUTE FUNCTION atlas_invalidate_vector_index();
COMMIT;
