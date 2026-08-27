import java.time.LocalDate;
import java.util.List;

/**
 * Checks filtering and chronological sorting for the upcoming command.
 */
public class UpcomingTaskTest {
    /**
     * Runs the upcoming-task checks using Java assertions.
     *
     * @param args command-line arguments; not used
     */
    public static void main(String[] args) {
        LocalDate today = LocalDate.of(2026, 8, 28);
        Task boundaryDeadline = new Deadline("boundary", "2026-09-04");
        Task pastDeadline = new Deadline("past", "2026-08-27");
        Task upcomingDeadline = new Deadline("submit report", "2026-09-02");
        Task todayDeadline = new Deadline("due today", "2026-08-28");
        Task upcomingEvent = new Event(
                "consultation", "2026-08-30 1400", "2026-08-30 1500");
        Task outsideDeadline = new Deadline("outside range", "2026-09-05");
        Task todo = new Todo("undated task");
        List<Task> tasks = List.of(boundaryDeadline, pastDeadline, upcomingDeadline,
                todayDeadline, upcomingEvent, outsideDeadline, todo);

        List<Task> upcomingTasks = Bola.findUpcomingTasks(tasks, today, 7);

        assert upcomingTasks.equals(List.of(
                todayDeadline, upcomingEvent, upcomingDeadline, boundaryDeadline));
        assert tasks.get(0) == boundaryDeadline : "The original task order must not change";
    }
}
