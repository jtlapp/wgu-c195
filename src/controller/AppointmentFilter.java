package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Appointment;
import model.User;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Base class of filters for the appointments tab on the main window, also serving
 * as a namespace for the different filters, implemented as subclasses. Each filter
 * shows a subset (possibly the entire set) of appointments and allows for navigating
 * to and from previous and subsequent subsets. Each of these subsets is a "view" of
 * the set of appointments, such a the view of one week or one month of appointments.
 * @author Joseph T. Lapp
 */

public abstract class AppointmentFilter {

    /**
     * A reference to the caller-maintained list of all possible appointments.
     */
    protected ObservableList<Appointment> allAppointments;

    /**
     * Constructs an appointment filter for the provided list of appointments.
     * @param allAppointments Appointments on which to base the filter
     */
    public AppointmentFilter(ObservableList<Appointment> allAppointments) {
        this.allAppointments = allAppointments;
    }

    /**
     * Subtitle to show next to the scene title to indicate which appointments
     * are being displayed. The subtitle for a filter may vary with the view's
     * current position within all possible appointments.
     * @return The subtitle for the active view
     */
    public abstract String getSubtitle();

    /**
     * Returns a list of appointments filtered for the current view.
     * @return List of appointments filtered for the current view
     */
    public ObservableList<Appointment> getFilteredList() {
        ObservableList<Appointment> filteredList = FXCollections.observableArrayList();
        allAppointments.stream()
                .filter(this::isInView)
                .forEach(filteredList::add);
        return filteredList;
    }

    /**
     * Indicates whether subsequent views follow the current view. Used for
     * determining whether to enable or disable view navigation buttons.
     * @return Whether there is a subsequent view
     */
    public abstract boolean hasNextView();

    /**
     * Indicates whether preceding views follow the current view. Used for
     * determining whether to enable or disable view navigation buttons.
     * @return Whether there is a preceding view
     */
    public abstract boolean hasPreviousView();

    /**
     * Advances the filter to the next available view. Only call if
     * hasNextView() reported an available view.
     */
    public abstract void nextView();

    /**
     * Advances the filter to the previous available view. Only call if
     * hasPreviousView() reported an available view.
     */
    public abstract void previousView();

    /**
     * Tests whether the provided appointment is in the current view.
     * @param appointment Appointment to test
     * @return Whether the appointment is in the current view
     */
    protected abstract boolean isInView(Appointment appointment);

    /**
     * Filter that does not filter at all, resulting in a display of all appointments.
     */
    public static class None extends AppointmentFilter {

        public None(ObservableList<Appointment> allAppointments) {
            super(allAppointments);
        }

        public String getSubtitle() { return null; }

        public boolean hasNextView() { return false; }

        public boolean hasPreviousView() { return false; }

        public void nextView() { /* not called */ }

        public void previousView() { /* not called */ }

        protected boolean isInView(Appointment appointment) { return true; }
    }

    /**
     * Filter of appointments by month, showing one month at a time. It tracks the
     * currently shown month and allows for navigation to previous and subsequent
     * months. It navigates through empty months between months having appointments
     * rather than jumping only to months having appointments.
     */
    public static class Monthly extends AppointmentFilter {

        private YearMonth month;

        public Monthly(ObservableList<Appointment> allAppointments) {
            super(allAppointments);
            month = getLastMonth();
            if (month == null) {
                month = YearMonth.now();
            }
        }

        public String getSubtitle() {
            return month.getMonth() + " " + month.getYear();
        }

        public boolean hasNextView() {
            var lastMonth = getLastMonth();
            return (lastMonth != null && lastMonth.isAfter(month));
        }

        public boolean hasPreviousView() {
            if (allAppointments.isEmpty()) {
                return false;
            }
            var firstAppointment = allAppointments.get(0);
            var firstMonth = YearMonth.from(firstAppointment.getStartTime());
            return firstMonth.isBefore(month);
        }

        public void nextView() {
            month = month.plusMonths(1);
        }

        public void previousView() {
            month = month.minusMonths(1);
        }

        protected boolean isInView(Appointment appointment) {
            var appointmentMonth = YearMonth.from(appointment.getStartTime());
            return appointmentMonth.equals(month);
        }

        private YearMonth getLastMonth() {
            if (allAppointments.isEmpty()) {
                return null;
            }
            var lastAppointment = allAppointments.get(allAppointments.size() - 1);
            return YearMonth.from(lastAppointment.getStartTime());
        }
    }

    /**
     * Filter of appointments by week, showing one week at a time. It tracks the
     * currently shown week and allows for navigation to previous and subsequent
     * weeks. It navigates through empty week between weeks having appointments
     * rather than jumping only to weeks having appointments.
     */
    public static class Weekly extends AppointmentFilter {

        private LocalDate firstDayOfWeek;

        public Weekly(ObservableList<Appointment> allAppointments) {
            super(allAppointments);
            firstDayOfWeek = getLastDate();
            if (firstDayOfWeek == null) {
                firstDayOfWeek = LocalDate.now();
            }
            var dayOfWeek = firstDayOfWeek.getDayOfWeek().getValue();
            if (dayOfWeek > 1) {
                firstDayOfWeek = firstDayOfWeek.minusDays(dayOfWeek - 1);
            }
        }

        public String getSubtitle() {
            return "Week of " + firstDayOfWeek.format(User.DATE_FORMAT);
        }

        public boolean hasNextView() {
            var lastDate = getLastDate();
            var firstDayOfNextWeek = firstDayOfWeek.plusDays(7);
            return lastDate != null && (lastDate.equals(firstDayOfNextWeek) ||
                    lastDate.isAfter(firstDayOfNextWeek));
        }

        public boolean hasPreviousView() {
            if (allAppointments.isEmpty()) {
                return false;
            }
            var firstAppointment = allAppointments.get(0);
            var firstDate = firstAppointment.getStartTime().toLocalDate();
            return firstDate.isBefore(firstDayOfWeek);
        }

        public void nextView() {
            firstDayOfWeek = firstDayOfWeek.plusDays(7);
        }

        public void previousView() {
            firstDayOfWeek = firstDayOfWeek.minusDays(7);
        }

        protected boolean isInView(Appointment appointment) {
            var appointmentDate = appointment.getStartTime().toLocalDate();
            return (appointmentDate.equals(firstDayOfWeek) ||
                    (appointmentDate.isAfter(firstDayOfWeek) &&
                            appointmentDate.isBefore(firstDayOfWeek.plusDays(7))));
        }

        private LocalDate getLastDate() {
            if (allAppointments.isEmpty()) {
                return null;
            }
            var lastAppointment = allAppointments.get(allAppointments.size() - 1);
            return lastAppointment.getStartTime().toLocalDate();
        }
    }
}
