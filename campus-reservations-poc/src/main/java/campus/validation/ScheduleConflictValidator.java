package campus.validation;
import campus.domain.ReservationRequest;
import campus.domain.Room;
import campus.domain.User;
import campus.services.SchedulingService;

public class ScheduleConflictValidator extends ValidationHandler {
    private final SchedulingService scheduling;
    public ScheduleConflictValidator(SchedulingService scheduling) { this.scheduling = scheduling; }

    @Override
    protected ValidationResult check(ReservationRequest req, User actor) {
        boolean free = scheduling.available(req.getRoomId(), req.getSlot());
        if (free) return ValidationResult.ok();
        Room alt = scheduling.suggest(req);
        return (alt != null) ? ValidationResult.ok(alt)
                             : ValidationResult.fail("Room/time conflict and no alternative available");
    }
}
