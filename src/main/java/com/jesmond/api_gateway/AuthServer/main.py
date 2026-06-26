from fastapi import FastAPI
import issue_rsa_key_pair
import issue_jwt_token
from cryptography.hazmat.primitives.asymmetric import rsa

app = FastAPI()
app.include_router(issue_rsa_key_pair.router)
app.include_router(issue_jwt_token.router)