package campus_poc.validation;
import campus_poc.domain.ReservationRequest;
import campus_poc.domain.Room;
import campus_poc.domain.User;
import campus_poc.services.SchedulingService;

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
