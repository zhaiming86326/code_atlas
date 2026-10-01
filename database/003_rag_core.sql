-- Stage 3 core: no vector extension needed for settings, audit and keyword QA.
BEGIN;
CREATE TABLE IF NOT EXISTS atlas_model_config (
  role text PRIMARY KEY CHECK(role IN ('LLM','EMBEDDING')),
  base_url text NOT NULL,
  model text NOT NULL,
  dimensions integer NOT NULL DEFAULT 1536 CHECK(dimensions BETWEEN 1 AND 16000),
  revision text NOT NULL DEFAULT '1',
  input_price_per_million numeric CHECK(input_price_per_million>=0),
  output_price_per_million numeric CHECK(output_price_per_million>=0),
  price_basis text NOT NULL DEFAULT '未知',
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS atlas_model_call (
  id uuid PRIMARY KEY,
  role text NOT NULL,
  model text NOT NULL,
  status text NOT NULL CHECK(status IN ('STARTED','SUCCESS','FAILED')),
  input_tokens integer,
  output_tokens integer,
  estimated_cost numeric,
  price_basis text NOT NULL,
  error text,
  created_at timestamptz NOT NULL DEFAULT now(),
  completed_at timestamptz
);
CREATE TABLE IF NOT EXISTS atlas_qa (
  id uuid PRIMARY KEY,
  question text NOT NULL,
  answer text NOT NULL,
  status text NOT NULL,
  search_mode text NOT NULL,
  filters jsonb NOT NULL,
  citations jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS atlas_call_time_idx ON atlas_model_call(created_at);
COMMIT;
