package blok2.daos.db;

import blok2.daos.ICalendarPeriodDao;
import blok2.helpers.LocationStatus;
import blok2.helpers.Pair;
import blok2.helpers.Resources;
import blok2.model.calendar.CalendarPeriod;
import blok2.model.calendar.Timeslot;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

@Service
public class DBCalendarPeriodDao extends DAO implements ICalendarPeriodDao {


    private final Logger logger = Logger.getLogger(DBCalendarPeriodDao.class.getSimpleName());

    @Override
    public List<CalendarPeriod> getCalendarPeriodsOfLocation(int locationId) throws SQLException {
        try (Connection conn = adb.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(Resources.databaseProperties.getString("get_calendar_periods"));
            pstmt.setInt(1, locationId);
            return getCalendarPeriodsFromPstmt(pstmt, conn);
        }
    }

    @Override
    public List<CalendarPeriod> getAllCalendarPeriods() throws SQLException {
        try (Connection conn = adb.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(Resources.databaseProperties.getString("get_all_calendar_periods"));
            return getCalendarPeriodsFromPstmt(pstmt, conn);
        }
    }

    public List<CalendarPeriod> getCalendarPeriodsInWeek(LocalDate firstDayOfWeek) throws SQLException {
        LocalDate lastDayOfWeek = firstDayOfWeek.plusWeeks(1);
        return getCalendarPeriodsInPeriod(firstDayOfWeek, lastDayOfWeek);
    }

    public List<CalendarPeriod> getCalendarPeriodsInPeriod(LocalDate start, LocalDate end) throws SQLException {
        try (Connection conn = adb.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(Resources.databaseProperties.getString("get_calendar_periods_in_period"));
            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));
            ResultSet rs = stmt.executeQuery();

            List<CalendarPeriod> periods = new ArrayList<>();

            while (rs.next()) {
                periods.add(createCalendarPeriod(rs, conn));
            }

            return periods;
        }

    }

    @Override
    public void addCalendarPeriods(List<CalendarPeriod> periods) throws SQLException {
        try (Connection conn = adb.getConnection()) {
            try {
                conn.setAutoCommit(false);

                for (CalendarPeriod calendarPeriod : periods) {
                    addCalendarPeriod(calendarPeriod, conn);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void addCalendarPeriod(CalendarPeriod calendarPeriod, Connection conn) throws SQLException {
        // If calendar period is not reservable, make sure to do set the timeslotLength
        if (!calendarPeriod.isReservable()) {
            calendarPeriod.setTimeslotLength(calendarPeriod.getOpenHoursDuration() / 60);
        }

        String[] generatedColumns = { Resources.databaseProperties.getString("calendar_period_id") };
        PreparedStatement pstmt = conn.prepareStatement(Resources.databaseProperties.getString("insert_calendar_period"), generatedColumns);
        prepareCalendarPeriodPstmt(calendarPeriod, pstmt);
        pstmt.setInt(10, calendarPeriod.getLocation().getNumberOfSeats());
        pstmt.execute();

        // set the id and seat count generated by the insert query
        ResultSet rs = pstmt.getGeneratedKeys();
        rs.next();
        calendarPeriod.setId(rs.getInt(1));
        calendarPeriod.setSeatCount(calendarPeriod.getLocation().getNumberOfSeats());

        // add all relevant time periods
        addTimeslots(calendarPeriod, conn);
    }

    private void addTimeslots(CalendarPeriod period, Connection conn) throws SQLException {
        if (period.isReservable())
            addReservableTimeslots(period, conn);
        else
            addNonReservableTimeslots(period, conn);

        fillTimeslotList(period, conn);
    }

    private void addReservableTimeslots(CalendarPeriod period, Connection conn) throws SQLException {
        // One per day (end day inclusive)
        for (LocalDate currDate = period.getStartsAt(); !currDate.isAfter(period.getEndsAt()); currDate = currDate.plusDays(1)) {
            // One per hour (end hour/rest of hour non inclusive)
            int timeslotCount = period.getOpenHoursDuration() / (60*period.getTimeslotLength());
            for (int sequenceNr = 0; sequenceNr < timeslotCount; sequenceNr += 1) {
                addTimeslotPeriod(sequenceNr, currDate, period, conn);
            }
        }
    }

    private void addNonReservableTimeslots(CalendarPeriod period, Connection conn) throws SQLException {
        // One per day (end day inclusive)
        for (LocalDate currDate = period.getStartsAt(); !currDate.isAfter(period.getEndsAt()); currDate = currDate.plusDays(1)) {
            addTimeslotPeriod(0, currDate, period, conn);
        }
    }

    private void removeTimeslots(CalendarPeriod period, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(Resources.databaseProperties.getString("delete_timeslots_of_calendar"));
        stmt.setInt(1, period.getId());
        stmt.execute();
    }

    private void addTimeslotPeriod(int seq_id, LocalDate date, CalendarPeriod period, Connection conn) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement(Resources.databaseProperties.getString("insert_timeslots"));
        prepareTimeslotPeriodPstmt(seq_id, date, period, period.getSeatCount(), pstmt);
        pstmt.execute();
    }

    @Override
    public void updateCalendarPeriods(List<CalendarPeriod> from, List<CalendarPeriod> to) throws SQLException {
        if (from.size() != to.size()) {
            logger.warning("Update calendar periods: from-list has different sizing as opposed to the to-list");
            return;
        }

        try (Connection conn = adb.getConnection()) {
            try {
                conn.setAutoCommit(false);

                for (int i = 0; i < from.size(); i++) {
                    updateCalendarPeriod(from.get(i).getId(), to.get(i), conn);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public void updateCalendarPeriod(CalendarPeriod to) throws SQLException {
        try (Connection conn = adb.getConnection()) {
            try {
                conn.setAutoCommit(false);
                updateCalendarPeriod(to.getId(), to, conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void updateCalendarPeriod(int calendarId, CalendarPeriod to, Connection conn) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement(Resources.databaseProperties.getString("update_calendar_period"));
        // set ...
        prepareCalendarPeriodPstmt(to, pstmt);
        // where ...
        pstmt.setInt(10, calendarId);
        pstmt.execute();

        // recalculate the timeslots
        removeTimeslots(to, conn);
        addTimeslots(to, conn);
    }

    @Override
    public void deleteCalendarPeriod(CalendarPeriod calendarPeriod) throws SQLException {
        try (Connection conn = adb.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(Resources.databaseProperties.getString("delete_calendar_period"));
            prepareCommonPartOfCalendarPeriodPstmt(calendarPeriod, pstmt);
            pstmt.setBoolean(6, calendarPeriod.isReservable());
            pstmt.setInt(7, calendarPeriod.getTimeslotLength());
            pstmt.execute();
        }
    }

    /**
     * The status of a location depends on the current or next timeslot:
     *     - no current or next timeslot? -> LocationStatus.CLOSED
     *     - now is within timeslot? -> LocationStatus.OPEN, send closing timestamp as string
     *     - now is before start of timeslot, but the date is today -> LocationStatus.CLOSED_ACTIVE, send opening timestamp as string
     *     - now is before start of timeslot, and date is not today -> LocationStatus.CLOSED_UPCOMING, send opening timestamp as string
     */
    @Override
    public Pair<LocationStatus, String> getStatus(int locationId) throws SQLException {
        try (Connection conn = adb.getConnection()) {
            return getStatus(locationId, conn);
        }
    }

    public static Pair<LocationStatus, String> getStatus(int locationId, Connection conn) throws SQLException {
        // DateTimeFormatter to format the next opening hour in a consistent manner
        DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        PreparedStatement pstmt = conn.prepareStatement(Resources.databaseProperties.getString("get_current_and_or_next_timeslot"));
        pstmt.setInt(1, locationId);
        ResultSet rs = pstmt.executeQuery();

        if (!rs.next()) {
            return new Pair<>(LocationStatus.CLOSED, "");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeslotStart = rs.getTimestamp(Resources.databaseProperties.getString("timeslot_start_timestamp")).toLocalDateTime();
        LocalDateTime timeslotEnd = rs.getTimestamp(Resources.databaseProperties.getString("timeslot_end_timestamp")).toLocalDateTime();

        if (now.isAfter(timeslotStart) && now.isBefore(timeslotEnd)) {
            return new Pair<>(LocationStatus.OPEN, timeslotEnd.format(outputFormat));
        } else if (now.isBefore(timeslotStart) && now.toLocalDate().isEqual(timeslotStart.toLocalDate())) {
            return new Pair<>(LocationStatus.CLOSED_ACTIVE, timeslotStart.format(outputFormat));
        } else {
            return new Pair<>(LocationStatus.CLOSED_UPCOMING, timeslotStart.format(outputFormat));
        }
    }

    public CalendarPeriod getById(int calendarId) throws SQLException {
        try (Connection conn = adb.getConnection()) {
            PreparedStatement statement = conn.prepareStatement(Resources.databaseProperties.getString("get_calendar_period_by_id"));
            statement.setInt(1,calendarId);
            ResultSet set = statement.executeQuery();
            set.next();
            return createCalendarPeriod(set, conn);
        }
    }

    private void fillTimeslotList(CalendarPeriod calendarPeriod, Connection conn) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement(Resources.databaseProperties.getString("get_timeslots"));
        pstmt.setInt(1, calendarPeriod.getId());
        ResultSet rs = pstmt.executeQuery();

        List<Timeslot> timeslotList = new ArrayList<>();

        while(rs.next()) {
            timeslotList.add(createTimeslot(rs, conn));
        }

        calendarPeriod.setTimeslots(Collections.unmodifiableList(timeslotList));
    }

    public static CalendarPeriod createCalendarPeriod(ResultSet rs, Connection conn) throws SQLException {
        CalendarPeriod calendarPeriod = new CalendarPeriod();

        calendarPeriod.setStartsAt(rs.getDate(Resources.databaseProperties.getString("calendar_period_starts_at")).toLocalDate());
        calendarPeriod.setEndsAt(rs.getDate(Resources.databaseProperties.getString("calendar_period_ends_at")).toLocalDate());
        calendarPeriod.setOpeningTime(rs.getTime(Resources.databaseProperties.getString("calendar_period_opening_time")).toLocalTime());
        calendarPeriod.setClosingTime(rs.getTime(Resources.databaseProperties.getString("calendar_period_closing_time")).toLocalTime());
        calendarPeriod.setReservableFrom(rs.getTimestamp(Resources.databaseProperties.getString("calendar_period_reservable_from")).toLocalDateTime());
        calendarPeriod.setReservable(rs.getBoolean(Resources.databaseProperties.getString("calendar_period_reservable")));
        calendarPeriod.setId(rs.getInt(Resources.databaseProperties.getString("calendar_period_id")));
        calendarPeriod.setTimeslotLength(rs.getInt(Resources.databaseProperties.getString("calendar_period_timeslot_length")));
        calendarPeriod.setLocation(DBLocationDao.createLocation(rs,conn));
        calendarPeriod.setLockedFrom(rs.getTimestamp(Resources.databaseProperties.getString("calendar_period_locked_from")).toLocalDateTime());
        calendarPeriod.setSeatCount(rs.getInt(Resources.databaseProperties.getString("calendar_period_seat_count")));

        return calendarPeriod;
    }

    public static Timeslot createTimeslot(ResultSet rs, Connection conn) throws SQLException {
        int calendarId = (rs.getInt(Resources.databaseProperties.getString("timeslot_calendar_id")));
        int seqnr = (rs.getInt(Resources.databaseProperties.getString("timeslot_sequence_number")));
        LocalDate date = (rs.getDate(Resources.databaseProperties.getString("timeslot_date")).toLocalDate());

        Timeslot timeslot = new Timeslot(calendarId, seqnr, date, 0);

        int count = rs.getInt(Resources.databaseProperties.getString("timeslot_reservation_count"));
        int seatCount = rs.getInt(Resources.databaseProperties.getString("timeslot_seat_count"));

        timeslot.setAmountOfReservations(Math.min(count, seatCount));
        // Small hack to display near-as-correct info
        timeslot.setSeatCount(seatCount);

        return timeslot;
    }

    private void prepareCommonPartOfCalendarPeriodPstmt(CalendarPeriod calendarPeriod,
                                                        PreparedStatement pstmt) throws SQLException {
        pstmt.setInt(1, calendarPeriod.getLocation().getLocationId());
        pstmt.setDate(2, Date.valueOf(calendarPeriod.getStartsAt()));
        pstmt.setDate(3, Date.valueOf(calendarPeriod.getEndsAt()));
        pstmt.setTime(4, Time.valueOf(calendarPeriod.getOpeningTime()));
        pstmt.setTime(5, Time.valueOf(calendarPeriod.getClosingTime()));
    }

    private void prepareCalendarPeriodPstmt(CalendarPeriod calendarPeriod,
                                            PreparedStatement pstmt) throws SQLException {
        prepareCommonPartOfCalendarPeriodPstmt(calendarPeriod, pstmt);
        if(calendarPeriod.getReservableFrom() != null) {
            pstmt.setTimestamp(6, Timestamp.valueOf(calendarPeriod.getReservableFrom()));
        } else {
            pstmt.setNull(6, Types.TIMESTAMP);
        }
        pstmt.setBoolean(7, calendarPeriod.isReservable());
        pstmt.setInt(8, calendarPeriod.getTimeslotLength());
        pstmt.setTimestamp(9, Timestamp.valueOf(calendarPeriod.getLockedFrom()));
    }

    private void prepareTimeslotPeriodPstmt(int seq_id, LocalDate date, CalendarPeriod period, int amountOfSeats, PreparedStatement pstmt) throws SQLException {
        pstmt.setInt(1, period.getId());
        pstmt.setInt(2, seq_id);
        pstmt.setDate(3, Date.valueOf(date));
        pstmt.setInt(4, amountOfSeats);
    }

    private List<CalendarPeriod> getCalendarPeriodsFromPstmt(PreparedStatement pstmt, Connection conn) throws SQLException {
        ResultSet rs = pstmt.executeQuery();

        List<CalendarPeriod> periods = new ArrayList<>();

        while (rs.next()) {
            periods.add(createCalendarPeriod(rs, conn));
        }

        for (CalendarPeriod p : periods) {
            if(p.isReservable())
                fillTimeslotList(p, conn);
        }

        return periods;
    }
}
