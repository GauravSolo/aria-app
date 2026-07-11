# backend/ — Supabase

The Supabase database for Aria: schema, Row Level Security policies, and indexes.
Both the mobile (Expo) and macOS apps sync against this.

- `schema.sql` — full schema (tables, enums, RLS, indexes, triggers). _Added in step 2._

## Setup (summary — full steps in docs/SETUP.md later)
1. Create a free project at https://supabase.com.
2. In the SQL editor, paste & run `schema.sql`.
3. Copy your Project URL + `anon` public key into `mobile/.env` and the Mac app config.

See [../docs/DATA_MODEL.md](../docs/DATA_MODEL.md) for the authoritative data contract.
