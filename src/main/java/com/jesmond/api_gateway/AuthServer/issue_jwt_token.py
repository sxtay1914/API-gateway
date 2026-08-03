from fastapi import APIRouter
import time
from cryptography.hazmat.primitives import serialization
import jwt

router = APIRouter()

with open("private_key.pem", "rb") as key_file:
    private_key = serialization.load_pem_private_key(
        key_file.read(),
        password=None,
    )


@router.get("/issue_jwt_token")
def issue_jwt_token() -> dict:
    """Issue a JWT token signed with the RSA private key."""
    # JWTClaimsSet
    payload = {
        "sub": "test_client_id",
        "iat": int(time.time()),
        "exp": int(time.time()) + 3600,  # Token expires in 1 hour
    }
    # Create a JWT token using the payload and sign it with the private key
    # JWS header
    token = jwt.encode(
        payload, private_key, algorithm="RS256", headers={"kid": "my-key-id"}
    )

    return {"token": token, "token_type": "Bearer"}

