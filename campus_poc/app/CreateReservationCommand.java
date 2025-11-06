package campus_poc.app;

import campus_poc.domain.ReservationRequest;
import campus_poc.domain.User;
import campus_poc.services.ReservationService;
import campus_poc.validation.ValidationResult;

public class CreateReservationCommand implements Command {
    private final ReservationRequest req;
    private final User actor;
    public CreateReservationCommand(ReservationRequest req, User actor) { this.req = req; this.actor = actor; }
    @Override public void execute(ReservationService service) {
        ValidationResult r = service.submit(req, actor);
        if (!r.ok) throw new IllegalStateException("Submit failed: " + r.msg);
    }
}
