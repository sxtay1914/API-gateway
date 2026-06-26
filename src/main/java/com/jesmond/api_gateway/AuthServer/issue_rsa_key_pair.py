from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization
from fastapi import APIRouter
import base64


def int_to_base64url(n: int) -> str:
    """Convert an integer to a base64-encoded string."""
    byte_length = (n.bit_length() + 7) // 8
    b = n.to_bytes(byte_length, byteorder='big')
    return base64.urlsafe_b64encode(b).decode('utf-8').rstrip('=')

with open("private_key.pem", "rb") as key_file:
    private_key = serialization.load_pem_private_key(
        key_file.read(),
        password=None,
    )


router = APIRouter()

@router.get("/.well-known/jwks.json")
def jwks() -> dict:
    """Generate a JSON Web Key Set (JWKS) from the RSA public key."""
    public_key = private_key.public_key()
    public_numbers = public_key.public_numbers()

    jwk = {
        "kty": "RSA",
        "kid": "my-key-id",
        "n": int_to_base64url(public_numbers.n),
        "e": int_to_base64url(public_numbers.e),
        "alg": "RS256",
    }

    jwks_res = {"keys": [jwk]}

    return jwks_res