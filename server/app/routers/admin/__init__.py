"""
Admin sub-package — modular admin endpoints.

P1 FIX #9: Split monolithic admin.py (794 lines) into focused modules:
  - dashboard.py: Dashboard stats and metrics
  - users.py: User management CRUD
  - moderation.py: Community moderation (approve/reject modules, reports)
  - notifications.py: Push notifications and in-app messaging
  - maintenance.py: Audit log, cleanup tasks, data exports
"""
from fastapi import APIRouter

from app.routers.admin.dashboard import router as dashboard_router
from app.routers.admin.users import router as users_router
from app.routers.admin.moderation import router as moderation_router
from app.routers.admin.notifications import router as notifications_router
from app.routers.admin.maintenance import router as maintenance_router
from app.routers.admin.store_management import router as store_management_router
from app.routers.admin.analytics import router as analytics_router

router = APIRouter(prefix="/admin", tags=["Admin"])

router.include_router(dashboard_router)
router.include_router(users_router)
router.include_router(moderation_router)
router.include_router(notifications_router)
router.include_router(maintenance_router)
router.include_router(store_management_router)
router.include_router(analytics_router)
