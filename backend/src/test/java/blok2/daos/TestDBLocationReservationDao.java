package blok2.daos;

import blok2.BaseTest;
import blok2.TestSharedMethods;
import blok2.helpers.Pair;
import blok2.model.Authority;
import blok2.model.calendar.CalendarPeriod;
import blok2.model.calendar.Timeslot;
import blok2.model.Building;
import blok2.model.reservables.Location;
import blok2.model.reservations.LocationReservation;
import blok2.model.users.User;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.*;

public class TestDBLocationReservationDao extends BaseTest {

    private final static Logger logger = LoggerFactory.getLogger(TestDBLocationReservationDao.class);

    @Autowired
    private ILocationReservationDao locationReservationDao;

    @Autowired
    private IAccountDao accountDao;

    @Autowired
    private ILocationDao locationDao;

    @Autowired
    private IAuthorityDao authorityDao;

    @Autowired
    private ICalendarPeriodDao calendarPeriodDao;

    @Autowired
    private IBuildingDao buildingDao;

    private Location testLocation;
    private Location testLocation2;
    private User testUser;
    private User testUser2;
    private List<CalendarPeriod> calendarPeriods;
    private CalendarPeriod calendarPeriod1Seat;
    private List<CalendarPeriod> calendarPeriodsForLocation2;

    @Override
    public void populateDatabase() throws SQLException {
        // setup test location objects
        Authority authority = TestSharedMethods.insertTestAuthority(authorityDao);
        Building testBuilding = buildingDao.addBuilding(TestSharedMethods.testBuilding());

        testLocation = TestSharedMethods.testLocation(authority.clone(), testBuilding);
        Location testLocation1Seat = TestSharedMethods.testLocation1Seat(authority.clone(), testBuilding);
        testLocation2 = TestSharedMethods.testLocation2(authority.clone(), testBuilding);

        testUser = TestSharedMethods.adminTestUser();
        testUser2 = TestSharedMethods.studentTestUser();
        calendarPeriods = TestSharedMethods.testCalendarPeriods(testLocation);
        calendarPeriod1Seat = TestSharedMethods.testCalendarPeriods(testLocation1Seat).get(0);
        calendarPeriodsForLocation2 = TestSharedMethods.testCalendarPeriods(testLocation2);

        // Add test objects to database
        TestSharedMethods.addTestUsers(accountDao, testUser, testUser2);
        locationDao.addLocation(testLocation);
        locationDao.addLocation(testLocation1Seat);
        locationDao.addLocation(testLocation2);

        CalendarPeriod[] cps = new CalendarPeriod[calendarPeriods.size()];
        cps = calendarPeriods.toArray(cps);
        TestSharedMethods.addCalendarPeriods(calendarPeriodDao, cps);
        TestSharedMethods.addCalendarPeriods(calendarPeriodDao, calendarPeriod1Seat);
        cps = calendarPeriodsForLocation2.toArray(cps);
        TestSharedMethods.addCalendarPeriods(calendarPeriodDao, cps);
    }

    @Test
    public void addLocationReservationTest() throws SQLException {
        // retrieve entries from database instead of using the added instances
        Location location = locationDao.getLocation(testLocation.getName());
        User u = accountDao.getUserById(testUser.getAugentID());
        Timeslot timeslot = calendarPeriods.get(0).getTimeslots().get(0);
        // check whether all retrieved instances equal to the added instances
        Assert.assertEquals("addLocationReservation, setup testLocation", testLocation, location);
        Assert.assertEquals("addLocationReservation, setup testUser", testUser, u);

        // Create LocationReservation
        LocationReservation lr = new LocationReservation(u, LocalDateTime.of(1970, 1, 1, 9, 0, 0), timeslot, null);

        // add LocationReservation to database
        locationReservationDao.addLocationReservationIfStillRoomAtomically(lr);

        // test whether LocationReservation has been added successfully
        LocationReservation rlr = locationReservationDao.getLocationReservation(u.getAugentID(), timeslot); // rlr = retrieved location reservation
        Assert.assertEquals("addLocationReservation, getLocationReservation", lr, rlr);

        List<LocationReservation> list = locationReservationDao.getAllLocationReservationsOfUser(u.getAugentID());
        Assert.assertEquals("addLocationReservation, getAllLocationReservationsOfUser", 1, list.size());

        // delete LocationReservation from database
        locationReservationDao.deleteLocationReservation(u.getAugentID(), timeslot);
        rlr = locationReservationDao.getLocationReservation(u.getAugentID(), timeslot);
        Assert.assertNull("addLocationReservationTest, delete LocationReservation", rlr);
    }

    @Test
    public void addLocationReservationButFullTest() throws SQLException {
        // retrieve entries from database instead of using the added instances
        User u = accountDao.getUserById(testUser.getAugentID());
        User u2 = accountDao.getUserById(testUser2.getAugentID());

        Timeslot timeslot = calendarPeriod1Seat.getTimeslots().get(0);

        LocationReservation lr = new LocationReservation(u, LocalDateTime.now(), timeslot, null);
        TestSharedMethods.addCalendarPeriods(calendarPeriodDao, calendarPeriods.get(0));
        Assert.assertTrue(locationReservationDao.addLocationReservationIfStillRoomAtomically(lr));
        lr = new LocationReservation(u, LocalDateTime.now(), timeslot, null);
        // This is a duplicate entry into the database. Shouldn't work.
        Assert.assertFalse(locationReservationDao.addLocationReservationIfStillRoomAtomically(lr));
        lr = new LocationReservation(u2, LocalDateTime.now(), timeslot, null);
        // This is a second user. Also shouldn't work.
        Assert.assertFalse(locationReservationDao.addLocationReservationIfStillRoomAtomically(lr));

        // It really really shouldn't be in the database.
        List<LocationReservation> reservations = locationReservationDao.getAllLocationReservationsOfUser(u2.getAugentID());
        Assert.assertEquals(0, reservations.size());
    }

    @Test
    public void getLocationReservationsAndCalendarPeriodOfUserTest() throws SQLException {
        User u = accountDao.getUserById(testUser.getAugentID()); // test user from db
        List<Pair<LocationReservation, CalendarPeriod>> elrs = new ArrayList<>(); // expected location reservations

        // Create first location reservation for user in testLocation
        CalendarPeriod cp0 = calendarPeriods.get(0);
        Timeslot t0 = cp0.getTimeslots().get(0);
        LocationReservation lr0 = new LocationReservation(u, LocalDateTime.now(), t0, null);
        elrs.add(new Pair<>(lr0, cp0));

        // Create a second location reservation for the user in testLocation2
        CalendarPeriod cp1 = calendarPeriodsForLocation2.get(1);
        Timeslot t1 = cp1.getTimeslots().get(1);
        LocationReservation lr1 = new LocationReservation(u, LocalDateTime.now(), t1, null);
        elrs.add(new Pair<>(lr1, cp1));

        // Add the location reservations to the db
        Assert.assertTrue(locationReservationDao.addLocationReservationIfStillRoomAtomically(lr0));
        Assert.assertTrue(locationReservationDao.addLocationReservationIfStillRoomAtomically(lr1));

        // Retrieve location reservations in combination with the locations
        List<Pair<LocationReservation, CalendarPeriod>> rlrs = locationReservationDao
                .getAllLocationReservationsAndCalendarPeriodsOfUser(u.getAugentID());

        // Sort expected and retrieved location reservations
        elrs.sort(Comparator.comparing(Pair::hashCode));
        rlrs.sort(Comparator.comparing(Pair::hashCode));

        Assert.assertEquals(elrs, rlrs);
    }

    // @Test
    public void scanStudentTest() throws SQLException {
        // retrieve entries from database instead of using the added instances
        Location location = locationDao.getLocation(testLocation.getName());
        User u1 = accountDao.getUserById(testUser.getAugentID());
        User u2 = accountDao.getUserById(testUser2.getAugentID());
        Timeslot timeslot = calendarPeriods.get(0).getTimeslots().get(0);

        // check whether all retrieved instances equal to the added instances
        Assert.assertEquals("scanStudentTest, setup testLocation", testLocation, location);
        Assert.assertEquals("scanStudentTest, setup testUser", testUser, u1);
        Assert.assertEquals("scanStudentTest, setup testUser2", testUser2, u2);

        // Make reservation for today
        LocalDate today = LocalDate.now();

        // Make reservations for users u1 and u2
        LocationReservation lr1 = new LocationReservation(u1, LocalDateTime.now(), timeslot, null);
        LocationReservation lr2 = new LocationReservation(u2, LocalDateTime.now(), timeslot, null);

        locationReservationDao.addLocationReservation(lr1);
        locationReservationDao.addLocationReservation(lr2);

        // count reserved seats
        long c = locationReservationDao.countReservedSeatsOfTimeslot(timeslot);
        Assert.assertEquals("scanStudentTest, count reserved seats", 2, c);

        // scan the users for the location on date
        locationReservationDao.scanStudent(testLocation.getName(), u1.getAugentID());
        LocationReservation rlr1 = locationReservationDao.getLocationReservation(u1.getAugentID(), timeslot);
        lr1.setAttended(true);
        Assert.assertEquals("scanStudentTest, u1 scanned", lr1, rlr1);

        // get present students
        List<LocationReservation> present = locationReservationDao.getPresentStudents(testLocation.getName(), today);
        Assert.assertEquals("scanStudentTest, present size", 1, present.size());
        Assert.assertEquals("scanStudentTest, present user", u1, present.get(0).getUser());

        // get absent students
        List<LocationReservation> absent = locationReservationDao.getAbsentStudents(testLocation.getName(), today);
        Assert.assertEquals("scanStudentTest, absent size", 1, absent.size());
        Assert.assertEquals("scanStudentTest, absent user", u2, absent.get(0).getUser());

        // set all students' attended = true for the date
        locationReservationDao.setAllStudentsOfLocationToAttended(testLocation.getName(), today);
        present = locationReservationDao.getPresentStudents(testLocation.getName(), today);
        Assert.assertEquals("scanStudentTest, present size after all attended", 2, present.size());
    }

    /**
     * Steps undertaken in this test:
     *     - create 500 test users
     *     - create a location that has 490 seats
     *     - create an upcoming calendar period
     *     - let each user make a reservation concurrently
     *     - test whether no constraints have been violated
     */
    //@Test
    public void concurrentReservationsTest() throws SQLException, InterruptedException {
        // some constants
        final int N_USERS = 50;
        final int N_SEATS = 46;

        // some variables that will be used
        Thread[] threads = new Thread[N_USERS];
        User[] users = new User[N_USERS];

        // Create N_USERS test users
        for (int i = 0; i < N_USERS; i++) {
            users[i] = TestSharedMethods.studentTestUser();
            users[i].setAugentID(users[i].getAugentID() + "" + i);
            users[i].setMail(i + "" + users[i].getMail());
            accountDao.directlyAddUser(users[i]);
        }
        logger.info(String.format("All %d users have been created.", N_USERS));

        // Create a location that has N_SEATS seats
        Building building = TestSharedMethods.testBuilding();
        building.setName("Building to test concurrent reservations");
        buildingDao.addBuilding(building);

        Location location = TestSharedMethods.testLocation(
                TestSharedMethods.insertTestAuthority("Test concurrent reservations",
                        "Authority to test concurrent reservations", authorityDao),
                building);
        location.setNumberOfSeats(N_SEATS);
        location.setName("Location to test concurrent reservations");
        locationDao.addLocation(location);
        logger.info("Location has been created");

        // Create an upcoming calendar period (timeslots will be created too)
        CalendarPeriod calendarPeriod = TestSharedMethods.upcomingCalendarPeriods(location);
        calendarPeriodDao.addCalendarPeriods(singletonList(calendarPeriod));
        logger.info("Calendar period has been created");

        Timeslot timeslot = calendarPeriod.getTimeslots().get(0);
        Assert.assertNotNull(timeslot);

        // Let each user make a reservation concurrently
        for (int i = 0; i < N_USERS; i++) {
            final int _i = i;
            threads[i] = new Thread(
                () -> {
                    LocationReservation lr = new LocationReservation(users[_i], LocalDateTime.now(), timeslot, null);
                    try {
                        boolean s = locationReservationDao.addLocationReservationIfStillRoomAtomically(lr);
                        if(!s)
                            System.out.println("didn't work in thread "+ _i);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            );

        }

        List<Thread> threada = Arrays.asList(threads);
        for (Thread thread: threada) {
            thread.start();
            logger.info("thread %3d has been started");

        }


        // Now, wait for all threads to be finished
        for (int i = 0; i < N_USERS; i++) {
            threads[i].join();
        }

        // Test whether no constraints have been violated
        long lrsCount = locationReservationDao.countReservedSeatsOfTimeslot(timeslot);
        Assert.assertEquals(N_SEATS, lrsCount);
    }

    /**
     * Steps undertaken in this test:
     *     - create 500 test users
     *     - create a location that has 490 seats
     *     - create an upcoming calendar period
     *     - let each user make a reservation concurrently
     *     - test whether no constraints have been violated
     */
    //@Test
    public void concurrentReservationsMultipleTimeslotsTest() throws SQLException, InterruptedException {
        // some constants
        final int N_USERS = 500;
        final int N_SEATS = 500;

        // some variables that will be used
        Thread[] threads = new Thread[N_USERS];
        User[] users = new User[N_USERS];
        Timeslot[] timeslots = new Timeslot[N_USERS]; // timeslots[i] will be assigned to user[i]

        // Create N_USERS test users
        for (int i = 0; i < N_USERS; i++) {
            users[i] = TestSharedMethods.studentTestUser();
            users[i].setAugentID(users[i].getAugentID() + "" + i);
            users[i].setMail(i + "" + users[i].getMail());
            accountDao.directlyAddUser(users[i]);
        }
        logger.info(String.format("All %d users have been created.", N_USERS));

        // Create a location that has N_SEATS seats
        Building building = TestSharedMethods.testBuilding();
        building.setName("Building to test concurrent reservations");
        buildingDao.addBuilding(building);

        Location location = TestSharedMethods.testLocation(
                TestSharedMethods.insertTestAuthority("Test concurrent reservations",
                        "Authority to test concurrent reservations", authorityDao),
                building);
        location.setNumberOfSeats(N_SEATS);
        location.setName("Location to test concurrent reservations");
        locationDao.addLocation(location);
        logger.info("Location has been created");

        // Create an upcoming calendar period with a small timeslot size (timeslots will be created too)
        CalendarPeriod calendarPeriod = TestSharedMethods.upcomingCalendarPeriods(location);
        // Reasoning behind timeslot size: 17h - 9h = 8h = 480 min, twee dagen = 960 min -> 16 min/timeslot -> 60 timeslots
        calendarPeriod.setTimeslotLength(16);
        calendarPeriodDao.addCalendarPeriods(singletonList(calendarPeriod));
        logger.info("Calendar period has been created");

        Assert.assertEquals(60, calendarPeriod.getTimeslots().size());
        for (int i = 0; i < N_USERS; i++) {
            timeslots[i] = calendarPeriod.getTimeslots().get(i % calendarPeriod.getTimeslots().size());
        }

        // Let each user make a reservation concurrently
        for (int i = 0; i < N_USERS; i++) {
            final int _i = i;
            threads[i] = new Thread(
                    () -> {
                        LocationReservation lr = new LocationReservation(users[_i], LocalDateTime.now(), timeslots[_i], null);
                        try {
                            boolean s = locationReservationDao.addLocationReservationIfStillRoomAtomically(lr);
                            if(!s)
                                System.out.println("Didn't manage...");
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
            );
        }

        for (Thread thread: threads ) {
            thread.start();
            logger.info("thread has been started");
        }

        // Now, wait for all threads to be finished
        for (int i = 0; i < N_USERS; i++) {
            threads[i].join();
        }

        // Test whether no constraints have been violated
        long lrsCount = 0;
        for (Timeslot timeslot : calendarPeriod.getTimeslots()) {
            lrsCount += locationReservationDao.countReservedSeatsOfTimeslot(timeslot);
        }
        Assert.assertEquals(N_USERS, lrsCount);
    }

    //@Test
    public void timingOfReservationTest() throws SQLException {
        // Create a location that has N_SEATS seats
        Building building = TestSharedMethods.testBuilding();
        building.setName("Building to test concurrent reservations");
        buildingDao.addBuilding(building);

        Location location = TestSharedMethods.testLocation(
                TestSharedMethods.insertTestAuthority("Test concurrent reservations",
                        "Authority to test concurrent reservations", authorityDao),
                building);
        location.setNumberOfSeats(10);
        location.setName("Location to test concurrent reservations");
        locationDao.addLocation(location);
        logger.info("Location has been created");

        // Create an upcoming calendar period with a small timeslot size (timeslots will be created too)
        CalendarPeriod calendarPeriod = TestSharedMethods.upcomingCalendarPeriods(location);
        // Reasoning behind timeslot size: 17h - 9h = 8h = 480 min, twee dagen = 960 min -> 16 min/timeslot -> 60 timeslots
        calendarPeriod.setTimeslotLength(16);
        calendarPeriodDao.addCalendarPeriods(singletonList(calendarPeriod));
        logger.info("Calendar period has been created");

        Timeslot timeslot = calendarPeriod.getTimeslots().get(0);
        Assert.assertNotNull(timeslot);

        long t = 0;
        for (int i = 0; i < 1000; i++) {
            LocationReservation lr = new LocationReservation(testUser, LocalDateTime.now(), timeslot, null);
            LocalDateTime start = LocalDateTime.now();
            locationReservationDao.addLocationReservationIfStillRoomAtomically(lr);
            LocalDateTime end = LocalDateTime.now();

            t += ChronoUnit.MILLIS.between(start, end);
            locationReservationDao.deleteLocationReservation(testUser.getAugentID(), timeslot);
        }
        logger.info(String.format("Avg timing = %d", t/1000));

        long count = locationReservationDao.countReservedSeatsOfTimeslot(timeslot);
        Assert.assertEquals(0, count);
    }
}
