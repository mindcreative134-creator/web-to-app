from app.database import SessionLocal
from app.models.user import User
from app.utils.password_crypto import get_password_hash, encrypt_password
import sys

def init_admin(email, username, password):
    db = SessionLocal()
    try:
        # Check if user exists
        user = db.query(User).filter(User.email == email).first()
        if user:
            print(f"User {email} already exists.")
            return

        hashed_pw = get_password_hash(password)
        enc_pw = encrypt_password(password)

        new_admin = User(
            email=email,
            username=username,
            password_hash=hashed_pw,
            encrypted_password=enc_pw,
            is_admin=True,
            is_active=True,
            is_pro=True,
            pro_plan="ultra_lifetime"
        )
        db.add(new_admin)
        db.commit()
        print(f"Successfully created admin: {username} ({email})")
    finally:
        db.close()

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python init_admin.py <email> <username> <password>")
    else:
        init_admin(sys.argv[1], sys.argv[2], sys.argv[3])
