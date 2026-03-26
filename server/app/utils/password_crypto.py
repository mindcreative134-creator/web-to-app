from passlib.context import CryptContext
import base64
from cryptography.fernet import Fernet
import os

# Password hashing
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def verify_password(plain_password, hashed_password):
    return pwd_context.verify(plain_password, hashed_password)

def get_password_hash(password):
    return pwd_context.hash(password)

# AES Encryption for admin visibility (optional, used in users.py)
# In production, use a consistent SECRET_KEY from environment
SECRET_KEY = os.getenv("CRYPTO_SECRET", Fernet.generate_key().decode())
cipher_suite = Fernet(SECRET_KEY.encode() if isinstance(SECRET_KEY, str) else SECRET_KEY)

def encrypt_password(password: str) -> str:
    if not password: return ""
    return cipher_suite.encrypt(password.encode()).decode()

def decrypt_password(token: str) -> str:
    if not token or token == "—": return "—"
    try:
        return cipher_suite.decrypt(token.encode()).decode()
    except:
        return "[Decryption Error]"
