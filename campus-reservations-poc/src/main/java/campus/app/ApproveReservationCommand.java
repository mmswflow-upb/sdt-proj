package campus.app;
import campus.services.ReservationService;
import campus.domain.User;
public class ApproveReservationCommand implements Command {
    private final String requestId;
    private final User actor;
    public ApproveReservationCommand(String requestId, User actor) { this.requestId = requestId; this.actor = actor; }
    @Override public void execute(ReservationService service) { service.approve(requestId, actor); }
}
