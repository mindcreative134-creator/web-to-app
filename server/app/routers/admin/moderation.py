"""Admin: Community moderation — modules, comments, reports."""
import logging
from app.utils.time import utcnow
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session, joinedload
from sqlalchemy import func, desc, update

from app.database import get_db
from app.dependencies import get_current_admin
from app.models.user import User
from app.models.module_store import StoreModule, ModuleComment
from app.models.report import ModuleReport
from app.schemas.common import ApiResponse
from app.routers.admin.helpers import audit

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("/community/stats", response_model=ApiResponse)
def moderation_stats(admin: User = Depends(get_current_admin), db: Session = Depends(get_db)):
    """Moderation dashboard stats."""
    now = utcnow()
    today_start = now.replace(hour=0, minute=0, second=0, microsecond=0)

    pending_reports = db.query(func.count(ModuleReport.id)).filter(
        ModuleReport.status == "pending").scalar() or 0
    pending_modules = db.query(func.count(StoreModule.id)).filter(
        StoreModule.is_approved == False).scalar() or 0
    resolved_today = db.query(func.count(ModuleReport.id)).filter(
        ModuleReport.reviewed_at >= today_start,
        ModuleReport.status != "pending").scalar() or 0
    banned_users = db.query(func.count(User.id)).filter(
        User.is_active == False).scalar() or 0

    return ApiResponse(data={
        "pending_reports": pending_reports,
        "pending_modules": pending_modules,
        "resolved_today": resolved_today,
        "banned_users": banned_users,
    })


@router.get("/community/modules", response_model=ApiResponse)
def list_pending_modules(
    status_filter: str = Query("pending", alias="status"),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    query = db.query(StoreModule).options(joinedload(StoreModule.author))
    if status_filter == "pending":
        query = query.filter(StoreModule.is_approved == False)
    elif status_filter == "approved":
        query = query.filter(StoreModule.is_approved == True)

    query = query.order_by(desc(StoreModule.created_at))
    total = query.count()
    modules = query.offset((page - 1) * page_size).limit(page_size).all()

    return ApiResponse(data={
        "total": total, "page": page, "page_size": page_size,
        "modules": [
            {
                "id": m.id, "name": m.name, "title": m.title,
                "module_type": m.module_type, "description": m.description,
                "is_approved": m.is_approved, "is_featured": m.is_featured,
                "author_username": m.author.username if m.author else "Unknown",
                "author_id": m.author_id,
                "downloads": m.downloads or 0,
                "created_at": m.created_at.isoformat() if m.created_at else None,
            }
            for m in modules
        ],
    })


@router.put("/community/modules/{module_id}/approve", response_model=ApiResponse)
def approve_module(module_id: int, admin: User = Depends(get_current_admin),
                   db: Session = Depends(get_db)):
    module = db.query(StoreModule).filter(StoreModule.id == module_id).first()
    if not module:
        raise HTTPException(404, "Module not found")
    module.is_approved = True
    audit(db, admin.id, "approve_module", "module", module_id)
    db.commit()
    return ApiResponse(message="Module approved")


@router.put("/community/modules/{module_id}/reject", response_model=ApiResponse)
def reject_module(module_id: int, admin: User = Depends(get_current_admin),
                  db: Session = Depends(get_db)):
    module = db.query(StoreModule).filter(StoreModule.id == module_id).first()
    if not module:
        raise HTTPException(404, "Module not found")
    module.is_approved = False
    audit(db, admin.id, "reject_module", "module", module_id)
    db.commit()
    return ApiResponse(message="Module rejected")


@router.put("/community/modules/{module_id}/feature", response_model=ApiResponse)
def toggle_featured(module_id: int, admin: User = Depends(get_current_admin),
                    db: Session = Depends(get_db)):
    module = db.query(StoreModule).filter(StoreModule.id == module_id).first()
    if not module:
        raise HTTPException(404, "Module not found")
    module.is_featured = not module.is_featured
    audit(db, admin.id, "toggle_featured", "module", module_id,
          {"is_featured": module.is_featured})
    db.commit()
    return ApiResponse(message=f"Module {'featured' if module.is_featured else 'unfeatured'}")


@router.delete("/community/modules/{module_id}", response_model=ApiResponse)
def admin_delete_module(module_id: int, admin: User = Depends(get_current_admin),
                        db: Session = Depends(get_db)):
    module = db.query(StoreModule).filter(StoreModule.id == module_id).first()
    if not module:
        raise HTTPException(404, "Module not found")
    author_id = module.author_id
    db.delete(module)
    db.execute(
        update(User).where(User.id == author_id)
        .values(published_modules_count=func.greatest(0, User.published_modules_count - 1))
    )
    audit(db, admin.id, "delete_module", "module", module_id)
    db.commit()
    return ApiResponse(message="Module deleted")


@router.delete("/community/comments/{comment_id}", response_model=ApiResponse)
def admin_delete_comment(comment_id: int, admin: User = Depends(get_current_admin),
                         db: Session = Depends(get_db)):
    comment = db.query(ModuleComment).filter(ModuleComment.id == comment_id).first()
    if not comment:
        raise HTTPException(404, "Comment not found")
    comment.is_deleted = True
    comment.content = ""
    db.execute(
        update(StoreModule).where(StoreModule.id == comment.module_id)
        .values(comment_count=func.greatest(0, StoreModule.comment_count - 1))
    )
    audit(db, admin.id, "delete_comment", "comment", comment_id)
    db.commit()
    return ApiResponse(message="Comment deleted")


# ─── Reports ───

@router.get("/community/reports", response_model=ApiResponse)
def list_reports(
    status_filter: str = Query("pending", alias="status"),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    query = db.query(ModuleReport)
    if status_filter:
        query = query.filter(ModuleReport.status == status_filter)
    query = query.order_by(desc(ModuleReport.created_at))
    total = query.count()
    reports = query.offset((page - 1) * page_size).limit(page_size).all()

    return ApiResponse(data={
        "total": total, "page": page, "page_size": page_size,
        "reports": [
            {
                "id": r.id, "reporter_id": r.reporter_id,
                "module_id": r.module_id, "comment_id": r.comment_id,
                "reason": r.reason, "description": r.description,
                "status": r.status,
                "created_at": r.created_at.isoformat() if r.created_at else None,
            }
            for r in reports
        ],
    })


@router.put("/community/reports/{report_id}/resolve", response_model=ApiResponse)
def resolve_report(
    report_id: int,
    action: str = Query(..., pattern="^(resolved|dismissed)$"),
    note: str = Query(None),
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    report = db.query(ModuleReport).filter(ModuleReport.id == report_id).first()
    if not report:
        raise HTTPException(404, "Report not found")
    report.status = action
    report.reviewed_by = admin.id
    report.reviewed_at = utcnow()
    report.resolution_note = note
    audit(db, admin.id, f"report_{action}", "report", report_id)
    db.commit()
    return ApiResponse(message=f"Report {action}")
