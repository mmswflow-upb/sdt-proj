package campus_poc.services;

import campus_poc.domain.*;
import campus_poc.eventing.*;
import campus_poc.infra.DatabaseConnection;
import campus_poc.validation.*;

public class ReservationService {
    private final DatabaseConnection db;
    private final EventBus eventBus;
    private final ValidationHandler submissionValidators;
    private final SchedulingService scheduling;
    private final FacultyPolicy policy;

    public ReservationService(DatabaseConnection db, EventBus eventBus, SchedulingService scheduling, FacultyPolicy policy) {
        this.db = db;
        this.eventBus = eventBus;
        this.scheduling = scheduling;
        this.policy = policy;
        this.submissionValidators = new FacultyPolicyValidator(policy).then(new ScheduleConflictValidator(scheduling));
    }

    public ValidationResult submit(ReservationRequest req, User actor) {
        ValidationResult vr = submissionValidators.validate(req, actor);
        if (!vr.ok) return vr;
        if (vr.chosen != null) req.setRoomId(vr.chosen.getRoomId());

        req.setStatus(ReservationStatus.PENDING);
        db.save(req);
        scheduling.block(req.getRoomId(), req.getSlot());
        eventBus.publish(new DomainEvent(DomainEventType.RESERVATION_REQUESTED, req.getRequestId(), req));
        return vr;
    }

    public void approve(String requestId, User actor) {
        if (actor.getRole() != Role.ADMIN) throw new IllegalStateException("Only admin can approve");
        if (!db.exists(requestId)) throw new IllegalArgumentException("Request not found");
        db.markApproved(requestId);
        eventBus.publish(new DomainEvent(DomainEventType.RESERVATION_APPROVED, requestId, null));
    }
}
