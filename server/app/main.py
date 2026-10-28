from __future__ import annotations

import base64
import hashlib
import hmac
import os
import re
import secrets
import sqlite3
import tempfile
import time
from contextlib import asynccontextmanager, contextmanager
from pathlib import Path
from typing import Annotated
from uuid import UUID, uuid4

from fastapi import Depends, FastAPI, Header, HTTPException, Request, Response, status
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field
import jwt
from jwt import PyJWKClient
from jwt.exceptions import InvalidTokenError
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa


ROOT = Path(__file__).resolve().parents[1]
DATA_DIR = Path(os.environ.get("SWASTHAI_DATA_DIR", ROOT / ".local-data")).resolve()
OBJECT_DIR = DATA_DIR / "objects"
DATABASE_PATH = DATA_DIR / "metadata.sqlite3"
APP_ENV = os.environ.get("SWASTHAI_ENV", "local")
DEV_AUTH_TOKEN = os.environ.get("SWASTHAI_DEV_AUTH_TOKEN", "")
DEV_USER_ID = os.environ.get("SWASTHAI_DEV_USER_ID", "local-owner")
# Development tokens remain available only when explicitly selected.
DEV_EXTRA_USERS = os.environ.get("SWASTHAI_DEV_EXTRA_USERS", "")
AUTH_MODE = os.environ.get("SWASTHAI_AUTH_MODE", "development")
AUTH_JWKS_URL = os.environ.get("SWASTHAI_AUTH_JWKS_URL", "")
AUTH_ISSUER = os.environ.get("SWASTHAI_AUTH_ISSUER", "")
AUTH_AUDIENCE = os.environ.get("SWASTHAI_AUTH_AUDIENCE", "")
AUTH_REQUIRE_VERIFIED_EMAIL = os.environ.get(
    "SWASTHAI_AUTH_REQUIRE_VERIFIED_EMAIL", "false" if APP_ENV == "local" else "true"
).lower() == "true"
JWT_ALGORITHMS = ["EdDSA"]
_jwks_client: PyJWKClient | None = None
MAX_CIPHERTEXT_BYTES = int(os.environ.get("SWASTHAI_MAX_CIPHERTEXT_BYTES", str(100 * 1024 * 1024)))
ENVELOPE_MAGIC = b"SWAI"
ENVELOPE_VERSION = 1
ENVELOPE_HEADER_BYTES = 5  # Magic + version. Followed by 12-byte nonce and AES-GCM output/tag.
MIN_ENVELOPE_BYTES = ENVELOPE_HEADER_BYTES + 12 + 16
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")


def _require_local_development() -> None:
    if APP_ENV != "local":
        raise RuntimeError(
            "This API build only implements local development adapters. "
            "Production mode requires a verified identity provider and AWS object-store adapter."
        )
    if AUTH_MODE == "development" and len(DEV_AUTH_TOKEN) < 32:
        raise RuntimeError("SWASTHAI_DEV_AUTH_TOKEN must be set to a random value of at least 32 characters.")
    if AUTH_MODE == "oidc":
        if not AUTH_JWKS_URL or not AUTH_ISSUER or not AUTH_AUDIENCE:
            raise RuntimeError("OIDC auth requires SWASTHAI_AUTH_JWKS_URL, SWASTHAI_AUTH_ISSUER, and SWASTHAI_AUTH_AUDIENCE.")
        if APP_ENV != "local" and not AUTH_JWKS_URL.startswith("https://"):
            raise RuntimeError("Production OIDC JWKS must use HTTPS.")
    elif AUTH_MODE != "development":
        raise RuntimeError("SWASTHAI_AUTH_MODE must be 'development' or 'oidc'.")


def _connect() -> sqlite3.Connection:
    conn = sqlite3.connect(DATABASE_PATH, timeout=10)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


@contextmanager
def _connection():
    conn = _connect()
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def _init_storage() -> None:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    OBJECT_DIR.mkdir(parents=True, exist_ok=True)
    with _connection() as conn:
        conn.executescript(
            """
            CREATE TABLE IF NOT EXISTS profiles (
                id TEXT PRIMARY KEY,
                owner_user_id TEXT NOT NULL,
                encrypted_profile BLOB NOT NULL,
                encrypted_recovery_envelope BLOB,
                created_at INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS records (
                id TEXT PRIMARY KEY,
                profile_id TEXT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
                encrypted_metadata BLOB NOT NULL,
                ciphertext_sha256 TEXT NOT NULL,
                ciphertext_size INTEGER NOT NULL,
                crypto_version INTEGER NOT NULL,
                object_key TEXT NOT NULL UNIQUE,
                state TEXT NOT NULL CHECK(state IN ('pending_upload', 'ready')),
                created_at INTEGER NOT NULL
            );
            CREATE INDEX IF NOT EXISTS records_by_profile ON records(profile_id, created_at DESC);
            CREATE TABLE IF NOT EXISTS doctor_shares (
                id TEXT PRIMARY KEY,
                profile_id TEXT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
                created_by TEXT NOT NULL,
                token_sha256 TEXT NOT NULL UNIQUE,
                encrypted_summary BLOB NOT NULL,
                expires_at INTEGER NOT NULL,
                consumed_at INTEGER,
                revoked_at INTEGER,
                created_at INTEGER NOT NULL
            );
            CREATE INDEX IF NOT EXISTS shares_by_profile ON doctor_shares(profile_id, created_at DESC);
            CREATE TABLE IF NOT EXISTS family_invites (
                id TEXT PRIMARY KEY,
                profile_id TEXT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
                inviter_user_id TEXT NOT NULL,
                invitee_user_id TEXT NOT NULL,
                role TEXT NOT NULL CHECK(role IN ('caregiver', 'read_only')),
                encrypted_key_grant BLOB NOT NULL,
                state TEXT NOT NULL CHECK(state IN ('pending', 'accepted', 'rejected', 'revoked')),
                expires_at INTEGER NOT NULL,
                accepted_at INTEGER,
                rejected_at INTEGER,
                revoked_at INTEGER,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            );
            CREATE INDEX IF NOT EXISTS family_invites_by_invitee ON family_invites(invitee_user_id, state, created_at DESC);
            CREATE INDEX IF NOT EXISTS family_invites_by_profile ON family_invites(profile_id, state, created_at DESC);
            CREATE TABLE IF NOT EXISTS family_access_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                profile_id TEXT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
                actor_user_id TEXT NOT NULL,
                subject_user_id TEXT,
                action TEXT NOT NULL,
                invite_id TEXT,
                occurred_at INTEGER NOT NULL
            );
            CREATE INDEX IF NOT EXISTS family_events_by_profile ON family_access_events(profile_id, occurred_at DESC);
            CREATE TABLE IF NOT EXISTS families (
                id TEXT PRIMARY KEY,
                head_user_id TEXT NOT NULL,
                join_password_salt BLOB NOT NULL,
                join_password_hash BLOB NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            );
            CREATE UNIQUE INDEX IF NOT EXISTS one_family_per_head ON families(head_user_id);
            CREATE TABLE IF NOT EXISTS family_memberships (
                family_id TEXT NOT NULL REFERENCES families(id) ON DELETE CASCADE,
                user_id TEXT NOT NULL UNIQUE,
                role TEXT NOT NULL CHECK(role IN ('head', 'member')),
                joined_at INTEGER NOT NULL,
                PRIMARY KEY(family_id, user_id)
            );
            CREATE INDEX IF NOT EXISTS family_members_by_family ON family_memberships(family_id, joined_at);
            CREATE TABLE IF NOT EXISTS family_blocked_accounts (
                family_id TEXT NOT NULL REFERENCES families(id) ON DELETE CASCADE,
                user_id TEXT NOT NULL,
                blocked_by TEXT NOT NULL,
                blocked_at INTEGER NOT NULL,
                PRIMARY KEY(family_id, user_id)
            );
            CREATE TABLE IF NOT EXISTS family_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                family_id TEXT NOT NULL REFERENCES families(id) ON DELETE CASCADE,
                actor_user_id TEXT NOT NULL,
                subject_user_id TEXT,
                action TEXT NOT NULL,
                occurred_at INTEGER NOT NULL
            );
            CREATE INDEX IF NOT EXISTS family_events_by_family ON family_events(family_id, occurred_at DESC);
            CREATE TABLE IF NOT EXISTS account_device_keys (
                user_id TEXT NOT NULL,
                device_id TEXT NOT NULL,
                key_algorithm TEXT NOT NULL CHECK(key_algorithm = 'RSA-OAEP-SHA256-MGF1-SHA1'),
                public_key_der BLOB NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                revoked_at INTEGER,
                PRIMARY KEY(user_id, device_id)
            );
            CREATE INDEX IF NOT EXISTS device_keys_by_user ON account_device_keys(user_id, revoked_at);
            """
        )
        profile_columns = {row["name"] for row in conn.execute("PRAGMA table_info(profiles)")}
        if "encrypted_recovery_envelope" not in profile_columns:
            conn.execute("ALTER TABLE profiles ADD COLUMN encrypted_recovery_envelope BLOB")


@asynccontextmanager
async def lifespan(_: FastAPI):
    _require_local_development()
    global _jwks_client
    if AUTH_MODE == "oidc":
        _jwks_client = PyJWKClient(AUTH_JWKS_URL, cache_keys=True, timeout=5)
    _init_storage()
    yield


app = FastAPI(
    title="SwasthAI Local API",
    version="0.1.0",
    description=(
        "Local development API. Stores client-encrypted blobs and encrypted metadata only. "
        "It is not a production deployment."
    ),
    lifespan=lifespan,
)


class ProfileCreate(BaseModel):
    encrypted_profile: str = Field(min_length=44, max_length=100_000)
    encrypted_recovery_envelope: str = Field(min_length=44, max_length=100_000)


class DevicePublicKeyUpload(BaseModel):
    key_algorithm: str = Field(pattern=r"^RSA-OAEP-SHA256-MGF1-SHA1$")
    public_key: str = Field(min_length=300, max_length=4_000)


class RecoveryEnvelopeUpdate(BaseModel):
    encrypted_recovery_envelope: str = Field(min_length=44, max_length=100_000)


class RecordCreate(BaseModel):
    encrypted_metadata: str = Field(min_length=44, max_length=100_000)
    ciphertext_sha256: str = Field(pattern=r"^[0-9a-f]{64}$")
    ciphertext_size: int = Field(ge=MIN_ENVELOPE_BYTES, le=MAX_CIPHERTEXT_BYTES)
    crypto_version: int = Field(default=ENVELOPE_VERSION, ge=ENVELOPE_VERSION, le=ENVELOPE_VERSION)


class DoctorShareCreate(BaseModel):
    encrypted_summary: str = Field(min_length=44, max_length=2_000_000)
    expires_in_minutes: int = Field(default=15, ge=1, le=15)


class DoctorShareRedeem(BaseModel):
    token: str = Field(min_length=32, max_length=200)


class FamilyJoin(BaseModel):
    family_id: str = Field(min_length=36, max_length=36)
    join_password: str = Field(min_length=32, max_length=64)


def _decode_envelope(value: str, field_name: str, expected_version: int = ENVELOPE_VERSION) -> bytes:
    try:
        payload = base64.b64decode(value, validate=True)
    except Exception as exc:
        raise HTTPException(status_code=422, detail=f"{field_name} must be base64-encoded encrypted envelope bytes") from exc
    _validate_envelope(payload, field_name, expected_version)
    return payload


def _validate_envelope(payload: bytes, field_name: str, expected_version: int = ENVELOPE_VERSION) -> None:
    if len(payload) < MIN_ENVELOPE_BYTES:
        raise HTTPException(status_code=422, detail=f"{field_name} is too short to contain the required encrypted envelope")
    _validate_envelope_header(payload[:ENVELOPE_HEADER_BYTES], field_name, expected_version)


def _validate_envelope_header(header: bytes, field_name: str, expected_version: int = ENVELOPE_VERSION) -> None:
    if len(header) < ENVELOPE_HEADER_BYTES or header[:4] != ENVELOPE_MAGIC or header[4] != expected_version:
        raise HTTPException(status_code=422, detail=f"{field_name} must use the SWAI encrypted envelope version {expected_version}")


def _user_id(authorization: Annotated[str | None, Header()] = None) -> str:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Bearer token required")
    supplied = authorization.removeprefix("Bearer ")
    if AUTH_MODE == "oidc":
        client = _jwks_client
        if client is None:
            raise HTTPException(status_code=503, detail="Authentication verifier is not ready")
        try:
            signing_key = client.get_signing_key_from_jwt(supplied)
            claims = jwt.decode(
                supplied,
                signing_key.key,
                algorithms=JWT_ALGORITHMS,
                audience=AUTH_AUDIENCE,
                issuer=AUTH_ISSUER,
                options={"require": ["exp", "iat", "sub", "iss", "aud"]},
            )
            user_id = claims.get("sub")
            if not isinstance(user_id, str) or not user_id or len(user_id) > 200:
                raise InvalidTokenError("Invalid subject")
            if AUTH_REQUIRE_VERIFIED_EMAIL and claims.get("emailVerified") is not True:
                raise InvalidTokenError("Email address is not verified")
            return user_id
        except (jwt.PyJWTError, ValueError, OSError) as exc:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid bearer token") from exc

    users = {DEV_AUTH_TOKEN: DEV_USER_ID}
    for entry in DEV_EXTRA_USERS.split(";"):
        if not entry or ":" not in entry:
            continue
        user_id, token = entry.split(":", 1)
        if user_id and token:
            users[token] = user_id
    for token, user_id in users.items():
        if hmac.compare_digest(supplied, token):
            return user_id
    raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid local development token")


CurrentUser = Annotated[str, Depends(_user_id)]


def _owned_profile(conn: sqlite3.Connection, profile_id: str, user_id: str) -> sqlite3.Row:
    row = conn.execute(
        "SELECT * FROM profiles WHERE id = ? AND owner_user_id = ?",
        (profile_id, user_id),
    ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="Profile not found")
    return row


def _owned_record(conn: sqlite3.Connection, record_id: str, user_id: str) -> sqlite3.Row:
    row = conn.execute(
        """SELECT r.* FROM records r JOIN profiles p ON p.id = r.profile_id
           WHERE r.id = ? AND p.owner_user_id = ?""",
        (record_id, user_id),
    ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="Record not found")
    return row


def _readable_profile(conn: sqlite3.Connection, profile_id: str, user_id: str) -> tuple[sqlite3.Row, str]:
    profile = conn.execute("SELECT * FROM profiles WHERE id = ?", (profile_id,)).fetchone()
    if profile is None:
        raise HTTPException(status_code=404, detail="Profile not found")
    if profile["owner_user_id"] == user_id:
        return profile, "owner"
    is_family_head = conn.execute(
        """SELECT 1 FROM family_memberships AS member
           JOIN family_memberships AS head ON head.family_id = member.family_id
           WHERE member.user_id = ? AND member.role = 'member'
             AND head.user_id = ? AND head.role = 'head' LIMIT 1""",
        (profile["owner_user_id"], user_id),
    ).fetchone()
    if is_family_head is not None:
        return profile, "family_head"
    grant = conn.execute(
        """SELECT role FROM family_invites
           WHERE profile_id = ? AND invitee_user_id = ? AND state = 'accepted'
           ORDER BY accepted_at DESC LIMIT 1""",
        (profile_id, user_id),
    ).fetchone()
    if grant is None:
        raise HTTPException(status_code=404, detail="Profile not found")
    return profile, grant["role"]


def _readable_record(conn: sqlite3.Connection, record_id: str, user_id: str) -> sqlite3.Row:
    row = conn.execute("SELECT * FROM records WHERE id = ?", (record_id,)).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="Record not found")
    _readable_profile(conn, row["profile_id"], user_id)
    return row


def _audit_family_event(
    conn: sqlite3.Connection,
    profile_id: str,
    actor_user_id: str,
    action: str,
    subject_user_id: str | None = None,
    invite_id: str | None = None,
) -> None:
    conn.execute(
        """INSERT INTO family_access_events(
            profile_id, actor_user_id, subject_user_id, action, invite_id, occurred_at
        ) VALUES (?, ?, ?, ?, ?, ?)""",
        (profile_id, actor_user_id, subject_user_id, action, invite_id, int(time.time())),
    )


@app.get("/healthz")
def healthz() -> dict[str, str]:
    return {"status": "ok", "environment": APP_ENV, "storage": "local-ciphertext-only"}


def _join_password_hash(password: str, salt: bytes) -> bytes:
    return hashlib.scrypt(password.encode("utf-8"), salt=salt, n=2**14, r=8, p=1, dklen=32)


def _family_membership(conn: sqlite3.Connection, family_id: str, user_id: str) -> sqlite3.Row:
    row = conn.execute(
        "SELECT * FROM family_memberships WHERE family_id = ? AND user_id = ?",
        (family_id, user_id),
    ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="Family not found")
    return row


def _audit_family_group_event(
    conn: sqlite3.Connection,
    family_id: str,
    actor_user_id: str,
    action: str,
    subject_user_id: str | None = None,
) -> None:
    conn.execute(
        "INSERT INTO family_events(family_id, actor_user_id, subject_user_id, action, occurred_at) VALUES (?, ?, ?, ?, ?)",
        (family_id, actor_user_id, subject_user_id, action, int(time.time())),
    )


@app.post("/api/v1/families", status_code=status.HTTP_201_CREATED)
def create_family(user_id: CurrentUser) -> dict[str, str | int]:
    family_id = str(uuid4())
    join_password = secrets.token_urlsafe(24)
    salt = secrets.token_bytes(16)
    password_hash = _join_password_hash(join_password, salt)
    now = int(time.time())
    try:
        with _connection() as conn:
            conn.execute("BEGIN IMMEDIATE")
            if conn.execute("SELECT 1 FROM family_memberships WHERE user_id = ?", (user_id,)).fetchone():
                raise HTTPException(status_code=409, detail="This account already belongs to a family")
            conn.execute(
                "INSERT INTO families(id, head_user_id, join_password_salt, join_password_hash, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                (family_id, user_id, salt, password_hash, now, now),
            )
            conn.execute(
                "INSERT INTO family_memberships(family_id, user_id, role, joined_at) VALUES (?, ?, 'head', ?)",
                (family_id, user_id, now),
            )
            _audit_family_group_event(conn, family_id, user_id, "family_created", user_id)
    except sqlite3.IntegrityError as exc:
        raise HTTPException(status_code=409, detail="This account already belongs to a family") from exc
    return {"family_id": family_id, "join_password": join_password, "role": "head", "created_at": now}


@app.post("/api/v1/families/join", status_code=status.HTTP_201_CREATED)
def join_family(payload: FamilyJoin, user_id: CurrentUser) -> dict[str, str | int]:
    now = int(time.time())
    with _connection() as conn:
        conn.execute("BEGIN IMMEDIATE")
        family = conn.execute(
            "SELECT id, join_password_salt, join_password_hash FROM families WHERE id = ?",
            (payload.family_id,),
        ).fetchone()
        if family is None:
            raise HTTPException(status_code=403, detail="Family credentials are invalid")
        supplied_hash = _join_password_hash(payload.join_password, family["join_password_salt"])
        if not hmac.compare_digest(supplied_hash, family["join_password_hash"]):
            raise HTTPException(status_code=403, detail="Family credentials are invalid")
        if conn.execute(
            "SELECT 1 FROM family_blocked_accounts WHERE family_id = ? AND user_id = ?",
            (payload.family_id, user_id),
        ).fetchone():
            raise HTTPException(status_code=403, detail="The family head has removed this account")
        if conn.execute("SELECT 1 FROM family_memberships WHERE user_id = ?", (user_id,)).fetchone():
            raise HTTPException(status_code=409, detail="This account already belongs to a family")
        conn.execute(
            "INSERT INTO family_memberships(family_id, user_id, role, joined_at) VALUES (?, ?, 'member', ?)",
            (payload.family_id, user_id, now),
        )
        _audit_family_group_event(conn, payload.family_id, user_id, "member_joined", user_id)
    return {"family_id": payload.family_id, "user_id": user_id, "role": "member", "joined_at": now}


@app.get("/api/v1/families")
def list_families(user_id: CurrentUser) -> list[dict[str, object]]:
    with _connection() as conn:
        rows = conn.execute(
            """SELECT f.id, f.head_user_id, fm.role, fm.joined_at, f.created_at
               FROM family_memberships fm JOIN families f ON f.id = fm.family_id
               WHERE fm.user_id = ? ORDER BY f.created_at DESC""",
            (user_id,),
        ).fetchall()
    return [
        {
            "family_id": row["id"],
            "head_user_id": row["head_user_id"],
            "role": row["role"],
            "joined_at": row["joined_at"],
            "created_at": row["created_at"],
        }
        for row in rows
    ]


@app.get("/api/v1/families/{family_id}")
def get_family(family_id: str, user_id: CurrentUser) -> dict[str, object]:
    with _connection() as conn:
        membership = _family_membership(conn, family_id, user_id)
        family = conn.execute("SELECT head_user_id, created_at FROM families WHERE id = ?", (family_id,)).fetchone()
        members = conn.execute(
            "SELECT user_id, role, joined_at FROM family_memberships WHERE family_id = ? ORDER BY joined_at",
            (family_id,),
        ).fetchall()
        blocked_members = conn.execute(
            "SELECT user_id, blocked_by, blocked_at FROM family_blocked_accounts WHERE family_id = ? ORDER BY blocked_at DESC",
            (family_id,),
        ).fetchall() if membership["role"] == "head" else []
    return {
        "family_id": family_id,
        "head_user_id": family["head_user_id"],
        "your_role": membership["role"],
        "created_at": family["created_at"],
        "members": [dict(member) for member in members],
        "blocked_members": [dict(member) for member in blocked_members],
    }


@app.put("/api/v1/devices/{device_id}/public-key", status_code=status.HTTP_204_NO_CONTENT)
def register_device_public_key(
    device_id: str,
    payload: DevicePublicKeyUpload,
    user_id: CurrentUser,
) -> Response:
    try:
        canonical_device_id = str(UUID(device_id))
        public_key_bytes = base64.b64decode(payload.public_key, validate=True)
        public_key = serialization.load_der_public_key(public_key_bytes)
    except Exception as exc:
        raise HTTPException(status_code=422, detail="Device ID or public key is invalid") from exc
    if not isinstance(public_key, rsa.RSAPublicKey) or public_key.key_size < 3072:
        raise HTTPException(status_code=422, detail="Device key must be an RSA public key of at least 3072 bits")
    now = int(time.time())
    with _connection() as conn:
        conn.execute(
            """INSERT INTO account_device_keys(user_id, device_id, key_algorithm, public_key_der, created_at, updated_at)
               VALUES (?, ?, ?, ?, ?, ?)
               ON CONFLICT(user_id, device_id) DO UPDATE SET
                   key_algorithm = excluded.key_algorithm,
                   public_key_der = excluded.public_key_der,
                   updated_at = excluded.updated_at,
                   revoked_at = NULL""",
            (user_id, canonical_device_id, payload.key_algorithm, public_key_bytes, now, now),
        )
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@app.delete("/api/v1/devices/{device_id}/public-key", status_code=status.HTTP_204_NO_CONTENT)
def revoke_device_public_key(device_id: str, user_id: CurrentUser) -> Response:
    try:
        canonical_device_id = str(UUID(device_id))
    except ValueError as exc:
        raise HTTPException(status_code=422, detail="Device ID is invalid") from exc
    with _connection() as conn:
        conn.execute(
            "UPDATE account_device_keys SET revoked_at = ?, updated_at = ? WHERE user_id = ? AND device_id = ? AND revoked_at IS NULL",
            (int(time.time()), int(time.time()), user_id, canonical_device_id),
        )
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@app.get("/api/v1/families/{family_id}/device-keys")
def list_family_device_keys(family_id: str, user_id: CurrentUser) -> list[dict[str, object]]:
    with _connection() as conn:
        _family_membership(conn, family_id, user_id)
        rows = conn.execute(
            """SELECT keys.user_id, memberships.role, keys.device_id, keys.key_algorithm,
                      keys.public_key_der, keys.created_at, keys.updated_at
               FROM family_memberships memberships
               JOIN account_device_keys keys ON keys.user_id = memberships.user_id
               WHERE memberships.family_id = ? AND keys.revoked_at IS NULL
               ORDER BY memberships.joined_at, keys.created_at""",
            (family_id,),
        ).fetchall()
        _audit_family_group_event(conn, family_id, user_id, "device_public_keys_listed")
    return [
        {
            "user_id": row["user_id"],
            "role": row["role"],
            "device_id": row["device_id"],
            "key_algorithm": row["key_algorithm"],
            "public_key": base64.b64encode(row["public_key_der"]).decode("ascii"),
            "created_at": row["created_at"],
            "updated_at": row["updated_at"],
        }
        for row in rows
    ]


@app.post("/api/v1/families/{family_id}/join-password/rotate")
def rotate_family_join_password(family_id: str, user_id: CurrentUser) -> dict[str, str | int]:
    join_password = secrets.token_urlsafe(24)
    salt = secrets.token_bytes(16)
    password_hash = _join_password_hash(join_password, salt)
    now = int(time.time())
    with _connection() as conn:
        conn.execute("BEGIN IMMEDIATE")
        membership = _family_membership(conn, family_id, user_id)
        if membership["role"] != "head":
            raise HTTPException(status_code=403, detail="Only the family head can rotate join credentials")
        conn.execute(
            "UPDATE families SET join_password_salt = ?, join_password_hash = ?, updated_at = ? WHERE id = ?",
            (salt, password_hash, now, family_id),
        )
        _audit_family_group_event(conn, family_id, user_id, "join_password_rotated")
    return {"family_id": family_id, "join_password": join_password, "rotated_at": now}


def _revoke_family_member_grants(
    conn: sqlite3.Connection,
    family_id: str,
    actor_user_id: str,
    member_user_id: str,
) -> None:
    now = int(time.time())
    rows = conn.execute(
        """SELECT fi.id, fi.profile_id FROM family_invites fi
            JOIN profiles p ON p.id = fi.profile_id
            WHERE fi.state IN ('pending', 'accepted')
              AND EXISTS (SELECT 1 FROM family_memberships m WHERE m.family_id = ? AND m.user_id = p.owner_user_id)
              AND EXISTS (SELECT 1 FROM family_memberships m WHERE m.family_id = ? AND m.user_id = fi.inviter_user_id)
              AND EXISTS (SELECT 1 FROM family_memberships m WHERE m.family_id = ? AND m.user_id = fi.invitee_user_id)
              AND (fi.inviter_user_id = ? OR fi.invitee_user_id = ?)""",
        (family_id, family_id, family_id, member_user_id, member_user_id),
    ).fetchall()
    for row in rows:
        conn.execute(
            "UPDATE family_invites SET state = 'revoked', encrypted_key_grant = X'', revoked_at = ?, updated_at = ? WHERE id = ?",
            (now, now, row["id"]),
        )
        _audit_family_event(conn, row["profile_id"], actor_user_id, "family_member_access_revoked", member_user_id, row["id"])


def _remove_family_member(family_id: str, actor_user_id: str, member_user_id: str, allow_self: bool) -> None:
    now = int(time.time())
    with _connection() as conn:
        conn.execute("BEGIN IMMEDIATE")
        actor = _family_membership(conn, family_id, actor_user_id)
        target = _family_membership(conn, family_id, member_user_id)
        if actor["role"] == "head":
            if target["role"] == "head":
                raise HTTPException(status_code=422, detail="The family head cannot be removed")
        elif not (allow_self and actor_user_id == member_user_id):
            raise HTTPException(status_code=403, detail="Only the family head can remove a member")
        _revoke_family_member_grants(conn, family_id, actor_user_id, member_user_id)
        if actor["role"] == "head" and actor_user_id != member_user_id:
            conn.execute(
                "INSERT OR REPLACE INTO family_blocked_accounts(family_id, user_id, blocked_by, blocked_at) VALUES (?, ?, ?, ?)",
                (family_id, member_user_id, actor_user_id, now),
            )
        conn.execute(
            "DELETE FROM family_memberships WHERE family_id = ? AND user_id = ?",
            (family_id, member_user_id),
        )
        action = "member_left" if actor_user_id == member_user_id else "member_removed"
        _audit_family_group_event(conn, family_id, actor_user_id, action, member_user_id)


@app.delete("/api/v1/families/{family_id}/members/{member_user_id}", status_code=status.HTTP_204_NO_CONTENT)
def remove_family_member(family_id: str, member_user_id: str, user_id: CurrentUser) -> Response:
    _remove_family_member(family_id, user_id, member_user_id, allow_self=False)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@app.post("/api/v1/families/{family_id}/members/{member_user_id}/unblock", status_code=status.HTTP_204_NO_CONTENT)
def unblock_family_member(family_id: str, member_user_id: str, user_id: CurrentUser) -> Response:
    with _connection() as conn:
        conn.execute("BEGIN IMMEDIATE")
        membership = _family_membership(conn, family_id, user_id)
        if membership["role"] != "head":
            raise HTTPException(status_code=403, detail="Only the family head can restore a removed account")
        conn.execute(
            "DELETE FROM family_blocked_accounts WHERE family_id = ? AND user_id = ?",
            (family_id, member_user_id),
        )
        _audit_family_group_event(conn, family_id, user_id, "member_unblocked", member_user_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@app.delete("/api/v1/families/{family_id}/membership", status_code=status.HTTP_204_NO_CONTENT)
def leave_family(family_id: str, user_id: CurrentUser) -> Response:
    _remove_family_member(family_id, user_id, user_id, allow_self=True)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@app.get("/api/v1/families/{family_id}/events")
def list_family_group_events(family_id: str, user_id: CurrentUser) -> list[dict[str, object]]:
    with _connection() as conn:
        _family_membership(conn, family_id, user_id)
        rows = conn.execute(
            "SELECT actor_user_id, subject_user_id, action, occurred_at FROM family_events WHERE family_id = ? ORDER BY occurred_at DESC LIMIT 200",
            (family_id,),
        ).fetchall()
    return [dict(row) for row in rows]


@app.get("/api/v1/profiles")
def list_profiles(user_id: CurrentUser) -> list[dict[str, object]]:
    with _connection() as conn:
        rows = conn.execute(
            """SELECT DISTINCT p.id, p.encrypted_profile, p.created_at,
                      CASE WHEN p.owner_user_id = ? THEN p.encrypted_recovery_envelope ELSE NULL END AS encrypted_recovery_envelope,
                      CASE WHEN p.owner_user_id = ? THEN 'owner'
                           WHEN EXISTS (
                               SELECT 1 FROM family_memberships member
                               JOIN family_memberships head ON head.family_id = member.family_id
                               WHERE member.user_id = p.owner_user_id AND member.role = 'member'
                                 AND head.user_id = ? AND head.role = 'head'
                           ) THEN 'family_head'
                           ELSE fi.role END AS access_role,
                      CASE WHEN p.owner_user_id = ? THEN 1 ELSE EXISTS (
                          SELECT 1 FROM family_invites key_grant
                          WHERE key_grant.profile_id = p.id AND key_grant.invitee_user_id = ?
                            AND key_grant.state = 'accepted'
                      ) END AS has_decryption_grant
               FROM profiles p
               LEFT JOIN family_invites fi ON fi.profile_id = p.id
                    AND fi.invitee_user_id = ? AND fi.state = 'accepted'
               WHERE p.owner_user_id = ? OR fi.id IS NOT NULL OR EXISTS (
                   SELECT 1 FROM family_memberships member
                   JOIN family_memberships head ON head.family_id = member.family_id
                   WHERE member.user_id = p.owner_user_id AND member.role = 'member'
                     AND head.user_id = ? AND head.role = 'head'
               )
               ORDER BY p.created_at""",
            (user_id, user_id, user_id, user_id, user_id, user_id, user_id),
        ).fetchall()
    return [
        {
            "profile_id": row["id"],
            "encrypted_profile": base64.b64encode(row["encrypted_profile"]).decode("ascii"),
            "encrypted_recovery_envelope": (
                base64.b64encode(row["encrypted_recovery_envelope"]).decode("ascii")
                if row["encrypted_recovery_envelope"] is not None else None
            ),
            "created_at": row["created_at"],
            "access_role": row["access_role"],
            "has_decryption_grant": bool(row["has_decryption_grant"]),
        }
        for row in rows
    ]


@app.post("/api/v1/profiles", status_code=status.HTTP_201_CREATED)
def create_profile(payload: ProfileCreate, user_id: CurrentUser) -> dict[str, str | int]:
    encrypted_profile = _decode_envelope(payload.encrypted_profile, "encrypted_profile")
    recovery_envelope = _decode_envelope(payload.encrypted_recovery_envelope, "encrypted_recovery_envelope")
    profile_id = str(uuid4())
    created_at = int(time.time())
    with _connection() as conn:
        conn.execute(
            """INSERT INTO profiles(
                id, owner_user_id, encrypted_profile, encrypted_recovery_envelope, created_at
            ) VALUES (?, ?, ?, ?, ?)""",
            (profile_id, user_id, encrypted_profile, recovery_envelope, created_at),
        )
    return {"profile_id": profile_id, "created_at": created_at}


@app.get("/api/v1/profiles/{profile_id}/recovery-envelope")
def get_recovery_envelope(profile_id: str, user_id: CurrentUser) -> dict[str, str]:
    with _connection() as conn:
        profile = _owned_profile(conn, profile_id, user_id)
    envelope = profile["encrypted_recovery_envelope"]
    if envelope is None:
        raise HTTPException(status_code=404, detail="Recovery envelope not found")
    return {"encrypted_recovery_envelope": base64.b64encode(envelope).decode("ascii")}


@app.put("/api/v1/profiles/{profile_id}/recovery-envelope", status_code=status.HTTP_204_NO_CONTENT)
def update_recovery_envelope(
    profile_id: str,
    payload: RecoveryEnvelopeUpdate,
    user_id: CurrentUser,
) -> Response:
    recovery_envelope = _decode_envelope(payload.encrypted_recovery_envelope, "encrypted_recovery_envelope")
    with _connection() as conn:
        _owned_profile(conn, profile_id, user_id)
        conn.execute(
            "UPDATE profiles SET encrypted_recovery_envelope = ? WHERE id = ?",
            (recovery_envelope, profile_id),
        )
        _audit_family_event(conn, profile_id, user_id, "recovery_envelope_updated", user_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


class FamilyInviteCreate(BaseModel):
    invitee_user_id: str = Field(min_length=1, max_length=200, pattern=r"^[A-Za-z0-9._:@-]+$")
    role: str = Field(default="read_only", pattern=r"^(caregiver|read_only)$")
    # Opaque public-key-encrypted grant. Server never receives the profile data key.
    encrypted_key_grant: str = Field(min_length=44, max_length=20_000)
    expires_in_hours: int = Field(default=72, ge=1, le=168)


@app.post("/api/v1/profiles/{profile_id}/family-invites", status_code=status.HTTP_201_CREATED)
def create_family_invite(profile_id: str, payload: FamilyInviteCreate, user_id: CurrentUser) -> dict[str, str | int]:
    if payload.invitee_user_id == user_id:
        raise HTTPException(status_code=422, detail="You cannot invite your own account")
    try:
        encrypted_grant = base64.b64decode(payload.encrypted_key_grant, validate=True)
    except Exception as exc:
        raise HTTPException(status_code=422, detail="encrypted_key_grant must be base64-encoded ciphertext") from exc
    if len(encrypted_grant) < 32:
        raise HTTPException(status_code=422, detail="encrypted_key_grant is too short")

    invite_id = str(uuid4())
    now = int(time.time())
    expires_at = now + payload.expires_in_hours * 3600
    with _connection() as conn:
        _owned_profile(conn, profile_id, user_id)
        existing = conn.execute(
            """SELECT id FROM family_invites WHERE profile_id = ? AND invitee_user_id = ?
               AND (state = 'accepted' OR (state = 'pending' AND expires_at > ?))""",
            (profile_id, payload.invitee_user_id, now),
        ).fetchone()
        if existing is not None:
            raise HTTPException(status_code=409, detail="This person already has a pending invite or active access")
        conn.execute(
            """INSERT INTO family_invites(
                id, profile_id, inviter_user_id, invitee_user_id, role, encrypted_key_grant,
                state, expires_at, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, 'pending', ?, ?, ?)""",
            (invite_id, profile_id, user_id, payload.invitee_user_id, payload.role, encrypted_grant,
             expires_at, now, now),
        )
        _audit_family_event(conn, profile_id, user_id, "invite_created", payload.invitee_user_id, invite_id)
    return {"invite_id": invite_id, "state": "pending", "expires_at": expires_at}


@app.get("/api/v1/family-invites")
def list_family_invites(user_id: CurrentUser) -> list[dict[str, object]]:
    now = int(time.time())
    with _connection() as conn:
        rows = conn.execute(
            """SELECT id, profile_id, inviter_user_id, role, expires_at, created_at
               FROM family_invites WHERE invitee_user_id = ? AND state = 'pending' AND expires_at > ?
               ORDER BY created_at DESC""",
            (user_id, now),
        ).fetchall()
    return [
        {
            "invite_id": row["id"],
            "profile_id": row["profile_id"],
            "inviter_user_id": row["inviter_user_id"],
            "role": row["role"],
            "expires_at": row["expires_at"],
            "created_at": row["created_at"],
        }
        for row in rows
    ]


@app.get("/api/v1/profiles/{profile_id}/family")
def list_profile_family(profile_id: str, user_id: CurrentUser) -> list[dict[str, object]]:
    with _connection() as conn:
        _owned_profile(conn, profile_id, user_id)
        rows = conn.execute(
            """SELECT invitee_user_id, role, state, expires_at, accepted_at,
                      rejected_at, revoked_at, created_at, updated_at
               FROM family_invites WHERE profile_id = ? ORDER BY created_at DESC""",
            (profile_id,),
        ).fetchall()
    now = int(time.time())
    return [
        {
            **dict(row),
            "state": "expired" if row["state"] == "pending" and row["expires_at"] <= now else row["state"],
        }
        for row in rows
    ]


def _resolve_invite(conn: sqlite3.Connection, invite_id: str, invitee_user_id: str) -> sqlite3.Row:
    row = conn.execute(
        "SELECT * FROM family_invites WHERE id = ? AND invitee_user_id = ?",
        (invite_id, invitee_user_id),
    ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="Invite not found")
    if row["state"] != "pending" or row["expires_at"] <= int(time.time()):
        raise HTTPException(status_code=409, detail="Invite is no longer pending or has expired")
    return row


@app.post("/api/v1/family-invites/{invite_id}/accept")
def accept_family_invite(invite_id: str, user_id: CurrentUser) -> dict[str, str | int]:
    now = int(time.time())
    with _connection() as conn:
        conn.execute("BEGIN IMMEDIATE")
        invite = _resolve_invite(conn, invite_id, user_id)
        conn.execute(
            "UPDATE family_invites SET state = 'accepted', accepted_at = ?, updated_at = ? WHERE id = ?",
            (now, now, invite_id),
        )
        _audit_family_event(conn, invite["profile_id"], user_id, "invite_accepted", user_id, invite_id)
    return {
        "invite_id": invite_id,
        "profile_id": invite["profile_id"],
        "role": invite["role"],
        "encrypted_key_grant": base64.b64encode(invite["encrypted_key_grant"]).decode("ascii"),
        "accepted_at": now,
    }


@app.post("/api/v1/family-invites/{invite_id}/reject", status_code=status.HTTP_204_NO_CONTENT)
def reject_family_invite(invite_id: str, user_id: CurrentUser) -> Response:
    now = int(time.time())
    with _connection() as conn:
        conn.execute("BEGIN IMMEDIATE")
        invite = _resolve_invite(conn, invite_id, user_id)
        conn.execute(
            "UPDATE family_invites SET state = 'rejected', encrypted_key_grant = X'', rejected_at = ?, updated_at = ? WHERE id = ?",
            (now, now, invite_id),
        )
        _audit_family_event(conn, invite["profile_id"], user_id, "invite_rejected", user_id, invite_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@app.delete("/api/v1/profiles/{profile_id}/family/{member_user_id}", status_code=status.HTTP_204_NO_CONTENT)
def revoke_family_access(profile_id: str, member_user_id: str, user_id: CurrentUser) -> Response:
    if member_user_id == user_id:
        raise HTTPException(status_code=422, detail="Profile owners cannot revoke themselves")
    now = int(time.time())
    with _connection() as conn:
        _owned_profile(conn, profile_id, user_id)
        rows = conn.execute(
            """SELECT id FROM family_invites WHERE profile_id = ? AND invitee_user_id = ?
               AND state IN ('pending', 'accepted')""",
            (profile_id, member_user_id),
        ).fetchall()
        if not rows:
            raise HTTPException(status_code=404, detail="Family access not found")
        conn.execute(
            """UPDATE family_invites SET state = 'revoked', encrypted_key_grant = X'', revoked_at = ?, updated_at = ?
               WHERE profile_id = ? AND invitee_user_id = ? AND state IN ('pending', 'accepted')""",
            (now, now, profile_id, member_user_id),
        )
        for row in rows:
            _audit_family_event(conn, profile_id, user_id, "access_revoked", member_user_id, row["id"])
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@app.get("/api/v1/profiles/{profile_id}/family-access-events")
def list_family_access_events(profile_id: str, user_id: CurrentUser) -> list[dict[str, object]]:
    with _connection() as conn:
        _owned_profile(conn, profile_id, user_id)
        rows = conn.execute(
            """SELECT actor_user_id, subject_user_id, action, invite_id, occurred_at
               FROM family_access_events WHERE profile_id = ? ORDER BY occurred_at DESC LIMIT 200""",
            (profile_id,),
        ).fetchall()
    return [dict(row) for row in rows]


@app.get("/api/v1/profiles/{profile_id}/records")
def list_records(profile_id: str, user_id: CurrentUser) -> list[dict[str, object]]:
    with _connection() as conn:
        _, access_role = _readable_profile(conn, profile_id, user_id)
        if access_role == "family_head":
            _audit_family_event(conn, profile_id, user_id, "family_head_records_listed")
        rows = conn.execute(
            """SELECT id, encrypted_metadata, ciphertext_sha256, ciphertext_size,
                      crypto_version, state, created_at
               FROM records WHERE profile_id = ? ORDER BY created_at DESC""",
            (profile_id,),
        ).fetchall()
    return [
        {
            "record_id": row["id"],
            "encrypted_metadata": base64.b64encode(row["encrypted_metadata"]).decode("ascii"),
            "ciphertext_sha256": row["ciphertext_sha256"],
            "ciphertext_size": row["ciphertext_size"],
            "crypto_version": row["crypto_version"],
            "state": row["state"],
            "created_at": row["created_at"],
        }
        for row in rows
    ]


@app.post("/api/v1/profiles/{profile_id}/records", status_code=status.HTTP_201_CREATED)
def create_record(profile_id: str, payload: RecordCreate, user_id: CurrentUser) -> dict[str, str | int]:
    encrypted_metadata = _decode_envelope(payload.encrypted_metadata, "encrypted_metadata")
    record_id = str(uuid4())
    created_at = int(time.time())
    object_key = f"{record_id}.blob"
    with _connection() as conn:
        _owned_profile(conn, profile_id, user_id)
        conn.execute(
            """INSERT INTO records(
                id, profile_id, encrypted_metadata, ciphertext_sha256, ciphertext_size,
                crypto_version, object_key, state, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, 'pending_upload', ?)""",
            (
                record_id, profile_id, encrypted_metadata, payload.ciphertext_sha256,
                payload.ciphertext_size, payload.crypto_version, object_key, created_at,
            ),
        )
    return {"record_id": record_id, "state": "pending_upload", "created_at": created_at}


@app.put("/api/v1/records/{record_id}/ciphertext")
async def upload_record_ciphertext(
    record_id: str,
    request: Request,
    user_id: CurrentUser,
    x_ciphertext_sha256: Annotated[str | None, Header()] = None,
) -> dict[str, str | int]:
    if not x_ciphertext_sha256 or not SHA256_RE.fullmatch(x_ciphertext_sha256):
        raise HTTPException(status_code=400, detail="X-Ciphertext-SHA256 must be a lowercase SHA-256 hex digest")

    with _connection() as conn:
        row = _owned_record(conn, record_id, user_id)
    if row["state"] != "pending_upload":
        raise HTTPException(status_code=409, detail="Record upload is not pending")
    if not hmac.compare_digest(x_ciphertext_sha256, row["ciphertext_sha256"]):
        raise HTTPException(status_code=422, detail="Upload checksum does not match the record manifest")
    if request.headers.get("content-type", "").split(";")[0].strip().lower() != "application/octet-stream":
        raise HTTPException(status_code=415, detail="Upload must be opaque application/octet-stream ciphertext")

    temp_path: Path | None = None
    digest = hashlib.sha256()
    size = 0
    try:
        with tempfile.NamedTemporaryFile(dir=OBJECT_DIR, prefix="upload-", delete=False) as temp_file:
            temp_path = Path(temp_file.name)
            async for block in request.stream():
                if not block:
                    continue
                size += len(block)
                if size > row["ciphertext_size"] or size > MAX_CIPHERTEXT_BYTES:
                    raise HTTPException(status_code=413, detail="Ciphertext exceeds the declared size or upload limit")
                digest.update(block)
                temp_file.write(block)

        if size != row["ciphertext_size"]:
            raise HTTPException(status_code=422, detail="Ciphertext size does not match the record manifest")
        if not hmac.compare_digest(digest.hexdigest(), row["ciphertext_sha256"]):
            raise HTTPException(status_code=422, detail="Ciphertext checksum verification failed")
        with temp_path.open("rb") as uploaded_file:
            _validate_envelope_header(uploaded_file.read(ENVELOPE_HEADER_BYTES), "ciphertext", row["crypto_version"])

        final_path = OBJECT_DIR / row["object_key"]
        os.replace(temp_path, final_path)
        temp_path = None
        with _connection() as conn:
            conn.execute("UPDATE records SET state = 'ready' WHERE id = ? AND state = 'pending_upload'", (record_id,))
    finally:
        if temp_path is not None:
            temp_path.unlink(missing_ok=True)

    return {"record_id": record_id, "state": "ready", "ciphertext_size": size}


@app.get("/api/v1/records/{record_id}/ciphertext")
def download_record_ciphertext(record_id: str, user_id: CurrentUser) -> Response:
    with _connection() as conn:
        row = _readable_record(conn, record_id, user_id)
        _audit_family_event(conn, row["profile_id"], user_id, "record_download_authorized", user_id)
    if row["state"] != "ready":
        raise HTTPException(status_code=409, detail="Record ciphertext is not available")
    path = OBJECT_DIR / row["object_key"]
    if not path.is_file():
        raise HTTPException(status_code=404, detail="Record ciphertext is missing from local storage")
    return FileResponse(
        path,
        media_type="application/octet-stream",
        filename=f"{record_id}.blob",
        headers={"Cache-Control": "no-store", "X-Content-Type-Options": "nosniff"},
    )


@app.delete("/api/v1/records/{record_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_record(record_id: str, user_id: CurrentUser) -> Response:
    with _connection() as conn:
        row = _owned_record(conn, record_id, user_id)
        conn.execute("DELETE FROM records WHERE id = ?", (record_id,))
    (OBJECT_DIR / row["object_key"]).unlink(missing_ok=True)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@app.post("/api/v1/profiles/{profile_id}/doctor-shares", status_code=status.HTTP_201_CREATED)
def create_doctor_share(
    profile_id: str,
    payload: DoctorShareCreate,
    user_id: CurrentUser,
) -> dict[str, str | int]:
    encrypted_summary = _decode_envelope(payload.encrypted_summary, "encrypted_summary")
    share_id = str(uuid4())
    token = secrets.token_urlsafe(32)
    now = int(time.time())
    expires_at = now + payload.expires_in_minutes * 60
    with _connection() as conn:
        _owned_profile(conn, profile_id, user_id)
        conn.execute(
            """INSERT INTO doctor_shares(
                id, profile_id, created_by, token_sha256, encrypted_summary,
                expires_at, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)""",
            (share_id, profile_id, user_id, hashlib.sha256(token.encode()).hexdigest(), encrypted_summary, expires_at, now),
        )
    # The raw token is returned once; only its hash is stored. The encryption key is client-only.
    return {"share_id": share_id, "token": token, "expires_at": expires_at, "uses": 1}


@app.post("/public/v1/doctor-shares/redeem")
def redeem_doctor_share(payload: DoctorShareRedeem) -> Response:
    token_hash = hashlib.sha256(payload.token.encode()).hexdigest()
    now = int(time.time())
    with _connection() as conn:
        conn.execute("BEGIN IMMEDIATE")
        row = conn.execute(
            """SELECT id, encrypted_summary FROM doctor_shares
               WHERE token_sha256 = ? AND expires_at > ? AND consumed_at IS NULL AND revoked_at IS NULL""",
            (token_hash, now),
        ).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail="Share is invalid, expired, revoked, or already used")
        conn.execute("UPDATE doctor_shares SET consumed_at = ? WHERE id = ?", (now, row["id"]))
        encrypted_summary = bytes(row["encrypted_summary"])
    return Response(
        content=encrypted_summary,
        media_type="application/octet-stream",
        headers={
            "Cache-Control": "no-store, max-age=0",
            "Pragma": "no-cache",
            "Referrer-Policy": "no-referrer",
            "X-Content-Type-Options": "nosniff",
        },
    )


@app.delete("/api/v1/doctor-shares/{share_id}", status_code=status.HTTP_204_NO_CONTENT)
def revoke_doctor_share(share_id: str, user_id: CurrentUser) -> Response:
    with _connection() as conn:
        row = conn.execute(
            """SELECT s.id FROM doctor_shares s JOIN profiles p ON p.id = s.profile_id
               WHERE s.id = ? AND p.owner_user_id = ?""",
            (share_id, user_id),
        ).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail="Share not found")
        conn.execute("UPDATE doctor_shares SET revoked_at = ? WHERE id = ?", (int(time.time()), share_id))
    return Response(status_code=status.HTTP_204_NO_CONTENT)
