# SwasthAI local API

This is the first local WSL implementation of the server contract. It stores opaque encrypted-envelope bytes for profile metadata, report metadata, report blobs, and doctor summaries. It does not perform OCR, extraction, embedding, model inference, or key management. Those remain on the phone.

## Local setup (WSL Ubuntu)

From the repository root in Ubuntu:

```bash
cd server
python3 -m venv .venv
. .venv/bin/activate
python -m pip install --upgrade pip
python -m pip install -e .
```

Install the Better Auth dependencies in a second terminal:

```bash
cd server/auth
npm install
```

Start the Better Auth account service in one terminal. On first run, it creates a private `.env.local` with a random signing secret and listens on `127.0.0.1:8766`:

```bash
cd server
./run-local-auth.sh
```

Start the encrypted-record API in another terminal. It verifies short-lived EdDSA JWTs against the Better Auth JWKS endpoint and listens on `127.0.0.1:8765`:

```bash
cd server
./run-local.sh
```

The environment file, SQLite database, and object blobs are ignored by Git and live under `server/`. Keep the server loopback-only. Do not expose this plain-HTTP development process to Wi-Fi, the public internet, or real patient data. For a phone connection, use a trusted TLS tunnel or configure a TLS-terminating local proxy before opening the service beyond loopback.

OpenAPI is served at `http://127.0.0.1:8765/docs`; health is `http://127.0.0.1:8765/healthz`.

Android Settings accepts separate Better Auth and records API origins. Configure the Better Auth origin identically to `BETTER_AUTH_URL`, `SWASTHAI_AUTH_ISSUER`, and the API's expected audience. Android keeps the Better Auth cookie encrypted with an Android Keystore key and requests a short-lived JWT for each protected API operation; passwords are not persisted. Release builds require HTTPS. Debug builds allow local development HTTP for loopback/emulator-host addresses only. Physical-device access requires a trusted TLS endpoint and matching issuer configuration.

## Contract outline

- Better Auth owns email/password accounts, sessions, verification keys, and JWT signing keys in its private SQLite database. Every family member signs in with their own account; the family join password is a separate, random, head-rotatable group credential and is stored only as a salted scrypt hash. The records API validates JWT signature, EdDSA algorithm, issuer, audience, expiration, issued-at, and subject against Better Auth JWKS. Static development tokens remain available only by explicitly selecting `SWASTHAI_AUTH_MODE=development`.
- The local service allows unverified email accounts so the flow can run without an SMTP provider. Production Better Auth requires email verification and a configured SMTP sender; its service rejects production startup without HTTPS, a strong secret, and SMTP configuration. Production API startup still remains disabled until its cloud-storage adapter is implemented.
- Account endpoints are mounted below `http://127.0.0.1:8766/api/auth`; Better Auth's JWT endpoint issues the token for API requests and `/api/auth/jwks` publishes public verification keys. Keep the service loopback-only during local development.
- Profile details, recovery-key envelopes, and record metadata are accepted only as base64-encoded encrypted envelopes. Cloud profile creation requires an encrypted recovery-key envelope; recovery fetch/update is owner-only, and shared family members never receive it.
- Envelope v1 uses the `SWAI` magic bytes, a version byte (`1`), a 12-byte AES-GCM nonce, then ciphertext plus the 16-byte GCM tag. The server validates the envelope marker and version; it cannot prove that the payload was actually encrypted or authenticate the tag.
- A report upload is a manifest followed by raw `application/octet-stream` ciphertext. The API verifies the declared byte count and SHA-256 before marking the upload ready.
- A doctor summary is posted as ciphertext. A random one-use token is returned once; only a hash is stored. The client keeps the summary decryption key and puts it in a QR URL fragment, which browsers do not send in HTTP requests. The redemption response is non-cacheable and expires after at most 15 minutes.
- A family head creates a family ID and one-time join password; another authenticated Better Auth user joins by presenting both. The head can list a member's encrypted profile and list/download its ciphertext records while both accounts remain in the family. Profile responses identify `access_role: family_head` and `has_decryption_grant` separately: the API ACL does not give the head plaintext access, and the member's profile key must still be wrapped for the head's device. The Android join/key exchange is not connected yet. Explicit accepted profile invites also allow the named account to list/download ciphertext. Both `caregiver` and `read_only` invite roles are read-only on the API. Only the profile owner can add/delete records or create doctor shares; the family head can manage family membership and rotate the join password. Head removal blocks that account from rejoining until the head explicitly unblocks it; members may leave and rejoin with current credentials.
- The owner can list current/past family invites and access events. Pending invites expire after at most seven days. Rejecting or revoking clears the server's stored encrypted key grant and prevents future API reads. Revocation cannot erase a key or ciphertext already copied to an authorized device; the client must rotate the profile key and re-encrypt records to revoke cryptographic access.
- API routes: `POST /api/v1/families` creates a head-owned family and returns the join password once; `POST /api/v1/families/join` joins using the family ID/password; `GET /api/v1/families` and `GET /api/v1/families/{family_id}` list membership; head `POST /api/v1/families/{family_id}/join-password/rotate` rotates credentials; head `DELETE /api/v1/families/{family_id}/members/{member_user_id}` removes and blocks a member; head `POST /api/v1/families/{family_id}/members/{member_user_id}/unblock` permits later rejoin; members may `DELETE /api/v1/families/{family_id}/membership` to leave; `GET /api/v1/families/{family_id}/events` reads the group audit. `GET /api/v1/profiles` returns owned, explicitly shared, and family-head-visible encrypted profiles, with a separate `has_decryption_grant` flag; head access to records is audited. Profile-level sharing remains owner-created `POST /api/v1/profiles/{profile_id}/family-invites`, invitee `GET /api/v1/family-invites`, and `POST /api/v1/family-invites/{invite_id}/accept|reject`; profile access can be listed/revoked at `/api/v1/profiles/{profile_id}/family` and `/api/v1/profiles/{profile_id}/family/{member_user_id}`.

## Security and production boundary

The envelope validation checks framing only, not cryptographic authenticity; the phone must encrypt/decrypt using AES-GCM, verify authentication tags and record hashes, and never upload plaintext. This API does not prove that the client encrypted correctly. Local HTTP is suitable only on loopback. Before production, deploy Better Auth behind TLS, move both SQLite stores to managed durable databases, replace local blobs with AWS object-storage adapters, audit every grant/share, add rate limits and abuse controls, and retain no plaintext or decryption keys. Production API startup is deliberately rejected until the cloud-storage adapter exists.

Android now wraps random profile keys with a non-exportable Android Keystore key, generates a 24-word BIP-39-format recovery phrase, and can make/restore an encrypted key envelope without storing the phrase. SwasthAI uses the phrase's decoded 256-bit entropy directly as the wrap key; it is an app recovery phrase, not an interoperable wallet seed. New local vault PDFs and previously stored plaintext PDFs are encrypted with streaming AES-GCM before they remain in the vault. The recovery phrase flow is not yet exposed in account UI, the recovery envelope is not yet sent to this API, and Android cloud record sync is not connected. Family public-key/device provisioning and key grant creation/decryption, key rotation on revoke, minor/guardian consent, and QR display/decryption remain pending. Do not describe cloud sync as end-to-end encrypted until the Android client encrypts every uploaded report and decrypts only on authorized devices.
