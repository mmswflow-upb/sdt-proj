package campus_poc.app;
import campus_poc.services.ReservationService;
public class CommandBus {
    private static  CommandBus INSTANCE;
    private final ReservationService service;
    private CommandBus(ReservationService service) { this.service = service; }
    public static CommandBus getInstance(ReservationService service) {
        if (INSTANCE == null) INSTANCE = new CommandBus(service);
        return INSTANCE;
    }
    public void dispatch(Command cmd) { cmd.execute(service); }
}
