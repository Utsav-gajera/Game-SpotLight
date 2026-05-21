# Deploying Game-SpotLight to Render (backend) and Vercel (frontend)

This document describes the steps to deploy the microservice backend to Render (with an Nginx public proxy) and the frontend to Vercel.

Prerequisites
- Git repo access
- Render account (with permission to create services and attach persistent disks)
- Vercel account
- Supabase service-role key (keep secret)

High-level flow
- Render: deploy backend services as internal services; deploy `nginx` as public web service that proxies to internal services. Keep `storage-service` attached to a persistent disk if you need local file persistence.
- Vercel: host frontend; set `VITE_API_URL` to the Render `nginx` public URL.

1) Prepare `render.yaml` (already added to repo)
- The repository contains `render.yaml` at the repository root under `Game-SpotLight/render.yaml`. You can use this manifest or create services in the Render dashboard.

2) Add Render secrets and environment variables
- Recommended secrets (use Render UI -> Environment -> Secrets or `render` CLI):
  - `SUPABASE_SERVICE_ROLE_KEY` (value: your Supabase service_role key) — used by `storage-service`.
  - `STORAGE_SUPABASE_URL` (value: https://<your-project>.supabase.co)
  - `GAME_MONGO_URI` (connection string for MongoDB Atlas)
  - `AUTH_DATABASE_URL`, `PURCHASE_DATABASE_URL` (Postgres URIs if used)
  - `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_SASL_USERNAME`, `KAFKA_SASL_PASSWORD` (if using managed Kafka)
  - `REDIS_URL` / `REDIS_PASSWORD`

3) Attach Persistent Disk (optional)
- If you want `storage-service` to persist `storage-index.json` and local file cache, attach a Render Persistent Disk and mount it at `/app/files` for the `storage-service`.

4) Deploy
- Option A (UI): Create new services in Render and set each service's repo, branch and Dockerfile path. Mark backend services as Internal and `nginx` as Public.
- Option B (render CLI): Install `render` CLI and run `render deploy` — Render will use `render.yaml` if present. See Render docs for exact commands.

5) Health checks
- Set service health checks to `/actuator/health` (Spring Boot) or `/_health` for `nginx`.

6) Domain & TLS
- Assign domain to the `nginx` public service in Render and configure DNS. Render will provision TLS.

7) Vercel (frontend)
- In Vercel project settings:
  - Set `VITE_API_URL` to `https://<nginx-render-service>.onrender.com` (or your custom domain)
  - Set `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY` if the frontend needs to interact with Supabase directly (anon key only). Do NOT set the Supabase service_role key in Vercel.
- Optionally add `vercel.json` in `client/` to proxy `/api/*` to the Render nginx public URL instead of setting `VITE_API_URL` (example provided in `client/vercel.json`).

8) Post-deploy verification
- Visit `https://<nginx>/api/_health` and backend health endpoints.
- From frontend (Vercel), request `/api/games/{id}/download-url` and inspect the response.

Troubleshooting
- If signed URL generation fails, check `STORAGE_SUPABASE_KEY` and that `STORAGE_SUPABASE_URL` points to the correct Supabase project with `game-files` bucket.
- Check Render logs for `storage-service` and `nginx` for 4xx/5xx errors.

Security notes
- Never commit `service_role` keys; store them in Render secrets.
- Keep `storage-service` internal and only expose `nginx` publicly.
