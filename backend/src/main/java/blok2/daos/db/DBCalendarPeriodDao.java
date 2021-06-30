package blok2.daos.db;

import blok2.daos.ICalendarPeriodDao;
import blok2.daos.ILocationDao;
import blok2.helpers.LocationStatus;
import blok2.helpers.Pair;
import blok2.model.calendar.CalendarPeriod;
import blok2.model.calendar.Timeslot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

@Service
public class DBCalendarPeriodDao implements ICalendarPeriodDao {

    private final Logger logger = Logger.getLogger(DBCalendarPeriodDao.class.getSimpleName());

    private final ConnectionProvider connectionProvider;
    private final ILocationDao locationDao;

    @Autowired
    public DBCalendarPeriodDao(ConnectionProvider connectionProvider,
                               ILocationDao locationDao) {
        this.connectionProvider = connectionProvider;
        this.locationDao = locationDao;
    }

    @Override
    public List<CalendarPeriod> getCalendarPeriodsOfLocation(int locationId) throws SQLException {
        try (Connection conn = connectionProvider.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("" +
                    "select cp.*, l.*, a.*, b.*" +
                    "from public.calendar_periods cp " +
                    "   join public.locations l " +
                    "       on l.location_id = cp.location_id " +
                    "   join public.authority a " +
                    "       on a.authority_id = l.authority_id " +
                    "   join public.buildings b " +
                    "       on b.building_id = l.building_id " +
                    "where cp.location_id = ? " +
                    "order by cp.starts_at, cp.opening_time;");
            pstmt.setInt(1, locationId);
            return getCalendarPeriodsFromPstmt(pstmt, conn);
        }
    }

    @Override
    public List<CalendarPeriod> getAllCalendarPeriods() throws SQLException {
        try (Connection conn = connectionProvider.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("" +
                    "select cp.*, l.*, b.*, a.*" +
                    "from public.calendar_periods cp " +
                    "   join public.locations l " +
                    "       on l.location_id = cp.location_id " +
                    "   join public.authority a " +
                    "       on a.authority_id = l.authority_id " +
                    "   join public.buildings b " +
                    "       on l.building_id = b.building_id " +
                    "order by to_date(cp.starts_at || ' ' || cp.opening_time, 'YYYY-MM-DD HH24:MI');"
            );
            return getCalendarPeriodsFromPstmt(pstmt, conn);
        }
    }

    public List<CalendarPeriod> getCalendarPeriodsInWeek(LocalDate firstDayOfWeek) throws SQLException {
        LocalDate lastDayOfWeek = firstDayOfWeek.plusWeeks(1);
        return getCalendarPeriodsInPeriod(firstDayOfWeek, lastDayOfWeek);
    }

    public List<CalendarPeriod> getCalendarPeriodsInPeriod(LocalDate start, LocalDate end) throws SQLException {
        try (Connection conn = connectionProvider.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("" +
                    "select * " +
                    "from public.calendar_periods cp " +
                    "   join public.locations l " +
                    "       on l.location_id = cp.location_id " +
                    "   join public.authority a " +
                    "       on a.authority_id = l.authority_id " +
                    "   join public.buildings b " +
                    "       on b.building_id = l.building_id " +
                    "where cp.starts_at > ? and cp.starts_at < ?;");
            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));
            ResultSet rs = stmt.executeQuery();

            List<CalendarPeriod> periods = new ArrayList<>();

            while (rs.next()) {
                periods.add(createCalendarPeriod(rs));
            }

            return periods;
        }

    }

    @Override
    public void addCalendarPeriods(List<CalendarPeriod> periods) throws SQLException {
        try (Connection conn = connectionProvider.getConnection()) {
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

        String[] generatedColumns = { "calendar_id" };
        PreparedStatement pstmt = conn.prepareStatement("" +
                "insert into public.calendar_periods(location_id, starts_at, ends_at, opening_time, closing_time, reservable_from, reservable, timeslot_length, locked_from, seat_count) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", generatedColumns);
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
        PreparedStatement stmt = conn.prepareStatement("delete from public.timeslots rt where rt.calendar_id = ?;");
        stmt.setInt(1, period.getId());
        stmt.execute();
    }

    private void addTimeslotPeriod(int seq_id, LocalDate date, CalendarPeriod period, Connection conn) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("" +
                "insert into public.timeslots(calendar_id, timeslot_sequence_number, timeslot_date, seat_count) " +
                "values (?, ?, ?, ?);");
        prepareTimeslotPeriodPstmt(seq_id, date, period, period.getSeatCount(), pstmt);
        pstmt.execute();
    }

    @Override
    public void updateCalendarPeriods(List<CalendarPeriod> from, List<CalendarPeriod> to) throws SQLException {
        if (from.size() != to.size()) {
            logger.warning("Update calendar periods: from-list has different sizing as opposed to the to-list");
            return;
        }

        try (Connection conn = connectionProvider.getConnection()) {
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
        try (Connection conn = connectionProvider.getConnection()) {
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
        PreparedStatement pstmt = conn.prepareStatement("" +
                "update public.calendar_periods " +
                "set location_id = ?, starts_at = ?, ends_at = ?, opening_time = ?, closing_time = ?, reservable_from = ?, reservable = ?, timeslot_length = ?, locked_from = ? " +
                "where calendar_id = ?;");
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
        try (Connection conn = connectionProvider.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("" +
                    "delete from public.calendar_periods " +
                    "where location_id = ? and starts_at = ? and ends_at = ? and opening_time = ? and closing_time = ? and reservable = ? and timeslot_length = ?;");
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
        try (Connection conn = connectionProvider.getConnection()) {
            return getStatus(locationId, conn);
        }
    }

    public static Pair<LocationStatus, String> getStatus(int locationId, Connection conn) throws SQLException {
        // DateTimeFormatter to format the next opening hour in a consistent manner
        DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        PreparedStatement pstmt = conn.prepareStatement("" +
                "with x as ( " +
                "    select rt.calendar_id, rt.timeslot_sequence_number, rt.timeslot_date, rt.reservation_count, rt.seat_count " +
                "         , cp.location_id, cp.starts_at, cp.ends_at, cp.opening_time, cp.closing_time, cp.reservable_from, cp.locked_from, cp.reservable, cp.timeslot_length, cp.seat_count " +
                "         , (rt.timeslot_date + cp.opening_time)::timestamp + interval '1 minute' * cp.timeslot_length * rt.timeslot_sequence_number as timeslot_start " +
                "         , (rt.timeslot_date + cp.opening_time)::timestamp + interval '1 minute' * cp.timeslot_length * (rt.timeslot_sequence_number + 1) as timeslot_end " +
                "    from timeslots rt " +
                "             join calendar_periods cp " +
                "                  on cp.calendar_id = rt.calendar_id " +
                "    where cp.location_id = ? " +
                "      and (rt.timeslot_date + cp.opening_time)::timestamp + interval '1 minute' * cp.timeslot_length * (rt.timeslot_sequence_number + 1) > now() " +
                "), y as ( " +
                "    select x.*, row_number() over(order by timeslot_start) n " +
                "    from x " +
                ") " +
                "select * " +
                "from y " +
                "where n = 1 " +
                "order by timeslot_start;");
        pstmt.setInt(1, locationId);
        ResultSet rs = pstmt.executeQuery();

        if (!rs.next()) {
            return new Pair<>(LocationStatus.CLOSED, "");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeslotStart = rs.getTimestamp("timeslot_start").toLocalDateTime();
        LocalDateTime timeslotEnd = rs.getTimestamp("timeslot_end").toLocalDateTime();

        if (now.isAfter(timeslotStart) && now.isBefore(timeslotEnd)) {
            return new Pair<>(LocationStatus.OPEN, timeslotEnd.format(outputFormat));
        } else if (now.isBefore(timeslotStart) && now.toLocalDate().isEqual(timeslotStart.toLocalDate())) {
            return new Pair<>(LocationStatus.CLOSED_ACTIVE, timeslotStart.format(outputFormat));
        } else {
            return new Pair<>(LocationStatus.CLOSED_UPCOMING, timeslotStart.format(outputFormat));
        }
    }

    public CalendarPeriod getById(int calendarId) throws SQLException {
        try (Connection conn = connectionProvider.getConnection()) {
            PreparedStatement statement = conn.prepareStatement("" +
                    "select * " +
                    "from public.calendar_periods cp " +
                    "   join public.locations l " +
                    "       on cp.location_id = l.location_id " +
                    "   join public.buildings b " +
                    "       on b.building_id = l.building_id " +
                    "   join public.authority a " +
                    "       on a.authority_id = l.authority_id " +
                    "where cp.calendar_id = ?;");
            statement.setInt(1,calendarId);
            ResultSet set = statement.executeQuery();
            set.next();
            CalendarPeriod p = createCalendarPeriod(set);
            fillTimeslotList(p, conn);
            return p;
        }
    }

    private void fillTimeslotList(CalendarPeriod calendarPeriod, Connection conn) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("" +
                "select rt.timeslot_sequence_number, rt.timeslot_date, rt.calendar_id, rt.reservation_count, rt.seat_count " +
                "from public.timeslots rt " +
                "where calendar_id = ? " +
                "order by rt.timeslot_date, rt.timeslot_sequence_number;");
        pstmt.setInt(1, calendarPeriod.getId());
        ResultSet rs = pstmt.executeQuery();

        List<Timeslot> timeslotList = new ArrayList<>();

        while(rs.next()) {
            timeslotList.add(createTimeslot(rs));
        }

        calendarPeriod.setTimeslots(Collections.unmodifiableList(timeslotList));
    }

    public CalendarPeriod createCalendarPeriod(ResultSet rs) throws SQLException {
        CalendarPeriod calendarPeriod = new CalendarPeriod();

        calendarPeriod.setStartsAt(rs.getDate("starts_at").toLocalDate());
        calendarPeriod.setEndsAt(rs.getDate("ends_at").toLocalDate());
        calendarPeriod.setOpeningTime(rs.getTime("opening_time").toLocalTime());
        calendarPeriod.setClosingTime(rs.getTime("closing_time").toLocalTime());
        calendarPeriod.setReservableFrom(rs.getTimestamp("reservable_from").toLocalDateTime());
        calendarPeriod.setReservable(rs.getBoolean("reservable"));
        calendarPeriod.setId(rs.getInt("calendar_id"));
        calendarPeriod.setTimeslotLength(rs.getInt("timeslot_length"));
        int locationId = rs.getInt("location_id");
        calendarPeriod.setLocation(locationDao.getLocationById(locationId));
        calendarPeriod.setLockedFrom(rs.getTimestamp("locked_from").toLocalDateTime());
        calendarPeriod.setSeatCount(rs.getInt("seat_count"));

        return calendarPeriod;
    }

    public static Timeslot createTimeslot(ResultSet rs) throws SQLException {
        int calendarId = (rs.getInt("calendar_id"));
        int seqnr = (rs.getInt("timeslot_sequence_number"));
        LocalDate date = (rs.getDate("timeslot_date").toLocalDate());

        Timeslot timeslot = new Timeslot(calendarId, seqnr, date, 0);

        int count = rs.getInt("reservation_count");
        int seatCount = rs.getInt("seat_count");

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
            periods.add(createCalendarPeriod(rs));
        }

        for (CalendarPeriod p : periods) {
            if(p.isReservable())
                fillTimeslotList(p, conn);
        }

        return periods;
    }

}
