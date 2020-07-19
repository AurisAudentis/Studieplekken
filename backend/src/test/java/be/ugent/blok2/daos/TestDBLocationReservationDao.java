package be.ugent.blok2.daos;

import be.ugent.blok2.TestSharedMethods;
import be.ugent.blok2.helpers.date.CustomDate;
import be.ugent.blok2.helpers.date.Day;
import be.ugent.blok2.helpers.date.Time;
import be.ugent.blok2.model.users.User;
import be.ugent.blok2.model.reservables.Location;
import be.ugent.blok2.model.reservations.LocationReservation;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles({"db", "test"})
public class TestDBLocationReservationDao {

    @Autowired
    private ILocationReservationDao locationReservationDao;

    @Autowired
    private IAccountDao accountDao;

    @Autowired
    private ILocationDao locationDao;

    private Location testLocation;
    private User testUser;
    private User testUser2;

    @Before
    public void setup() {
        // Use test database
        TestSharedMethods.setupTestDaoDatabaseCredentials(accountDao);
        TestSharedMethods.setupTestDaoDatabaseCredentials(locationDao);
        TestSharedMethods.setupTestDaoDatabaseCredentials(locationReservationDao);

        // setup test location objects
        testLocation = TestSharedMethods.testLocation();

        testUser = TestSharedMethods.employeeAdminTestUser();
        testUser2 = TestSharedMethods.studentEmployeeTestUser();

        // Add test objects to database
        TestSharedMethods.addTestUsers(accountDao, testUser, testUser2);
        locationDao.addLocation(testLocation);
    }

    @After
    public void cleanup() {
        // Remove test objects from database
        locationDao.deleteLocation(testLocation.getName());
        TestSharedMethods.removeTestUsers(accountDao, testUser2, testUser);

        // Use regular database
        locationReservationDao.useDefaultDatabaseConnection();
    }

    @Test
    public void addLocationReservationTest() {
        // retrieve entries from database instead of using the added instances
        Location location = locationDao.getLocation(testLocation.getName());
        User u = accountDao.getUserById(testUser.getAugentID());

        // check whether all retrieved instances equal to the added instances
        Assert.assertEquals("addLocationReservation, setup testLocation", testLocation, location);
        Assert.assertEquals("addLocationReservation, setup testUser", testUser, u);

        // Create LocationReservation
        CustomDate date = new CustomDate(1970, 1, 1, 9, 0, 0);
        LocationReservation lr = new LocationReservation(location, u, date);

        // add LocationReservation to database
        locationReservationDao.addLocationReservation(lr);

        // test whether LocationReservation has been added successfully
        LocationReservation rlr = locationReservationDao.getLocationReservation(u.getAugentID(), date); // rlr = retrieved location reservation
        Assert.assertEquals("addLocationReservation, getLocationReservation", lr, rlr);

        List<LocationReservation> list = locationReservationDao.getAllLocationReservationsOfLocation(testLocation.getName());
        Assert.assertEquals("addLocationReservation, getAllLocationReservationsOfLocation", 1, list.size());

        list = locationReservationDao.getAllLocationReservationsOfUser(u.getAugentID());
        Assert.assertEquals("addLocationReservation, getAllLocationReservationsOfUser", 1, list.size());

        // delete LocationReservation from database
        locationReservationDao.deleteLocationReservation(u.getAugentID(), date);
        rlr = locationReservationDao.getLocationReservation(u.getAugentID(), date);
        Assert.assertNull("addLocationReservationTest, delete LocationReservation", rlr);
    }

    @Test
    public void scanStudentTest() {
        // retrieve entries from database instead of using the added instances
        Location location = locationDao.getLocation(testLocation.getName());
        User u1 = accountDao.getUserById(testUser.getAugentID());
        User u2 = accountDao.getUserById(testUser2.getAugentID());

        // check whether all retrieved instances equal to the added instances
        Assert.assertEquals("scanStudentTest, setup testLocation", testLocation, location);
        Assert.assertEquals("scanStudentTest, setup testUser", testUser, u1);
        Assert.assertEquals("scanStudentTest, setup testUser2", testUser2, u2);

        // Open the location
        CustomDate today = CustomDate.today();
        Time openingHour = new Time(9, 0, 0);
        Time closingHour = new Time(17, 0, 0);
        Day d = new Day(today, openingHour, closingHour, today);
        List<Day> days = new ArrayList<>();
        days.add(d);
        be.ugent.blok2.helpers.date.Calendar calendar = new be.ugent.blok2.helpers.date.Calendar(days);
        locationDao.addCalendarDays(testLocation.getName(), calendar);

        // Make reservations for users u1 and u2
        LocationReservation lr1 = new LocationReservation(location, u1, today);
        LocationReservation lr2 = new LocationReservation(location, u2,today);

        locationReservationDao.addLocationReservation(lr1);
        locationReservationDao.addLocationReservation(lr2);

        // count reserved seats
        int c = locationReservationDao.countReservedSeatsOfLocationOnDate(testLocation.getName(), today);
        Assert.assertEquals("scanStudentTest, count reserved seats", 2, c);

        // scan the users for the location on date
        LocationReservation rlr1 = locationReservationDao.scanStudent(testLocation.getName(), u1.getAugentID());
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

        // set u1 to unattended
        locationReservationDao.setReservationToUnAttended(u1.getAugentID(), today);
        present = locationReservationDao.getPresentStudents(testLocation.getName(), today);
        Assert.assertEquals("scanStudentTest, present size after u1 unattended", 1, present.size());
        Assert.assertEquals("scanStudentTest, present user after u1 unattended should be u2", u2, present.get(0).getUser());
    }
}