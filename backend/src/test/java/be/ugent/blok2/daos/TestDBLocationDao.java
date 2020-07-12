package be.ugent.blok2.daos;

import be.ugent.blok2.TestSharedMethods;
import be.ugent.blok2.helpers.date.Calendar;
import be.ugent.blok2.helpers.date.Day;
import be.ugent.blok2.reservables.Location;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;

/**
 * Note: the test that combines scanner users with locations, is to be found in TestDBScannerLocation.java
 */

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles({"db", "test"})
public class TestDBLocationDao {

    @Autowired
    private ILocationDao locationDao;

    private Location testLocation;

    @Before
    public void setup() {
        // Use test database
        TestSharedMethods.setupTestDaoDatabaseCredentials(locationDao);

        // Setup test objects
        testLocation = TestSharedMethods.testLocation();

        // Add test objects to database
        locationDao.addLocation(testLocation);
    }

    @After
    public void cleanup() {
        // Remove test objects from database
        locationDao.deleteLocation(testLocation.getName());

        // Use regular database
        locationDao.useDefaultDatabaseConnection();
    }

    @Test
    public void addLocationTest() {
        Location l = locationDao.getLocationWithoutLockersAndCalendar(testLocation.getName());
        Assert.assertEquals("addLocation", testLocation, l);

        locationDao.deleteLocation(testLocation.getName());
        l = locationDao.getLocation(testLocation.getName());
        Assert.assertNull("addLocation, remove added test location", l);
    }

    @Test
    public void changeLocationTest() {
        Location changedTestLocation = testLocation.clone();
        changedTestLocation.setName("Changed Test Location");

        locationDao.changeLocation(testLocation.getName(), changedTestLocation);
        Location location = locationDao.getLocationWithoutLockersAndCalendar(changedTestLocation.getName());
        Assert.assertEquals("changeLocationTest, fetch location by changed name", changedTestLocation, location);

        location = locationDao.getLocationWithoutLockersAndCalendar(testLocation.getName());
        Assert.assertNull("changeLocationTest, old location name may not have an entry", location);

        locationDao.deleteLocation(changedTestLocation.getName());
    }

    @Test
    public void addLockersTest() {
        Location expectedLocation = testLocation.clone();
        int prev_n = expectedLocation.getNumberOfLockers();

        // test adding positive amount of lockers
        int n = 10;
        expectedLocation.setNumberOfLockers(prev_n + n);
        locationDao.addLockers(testLocation.getName(), n);
        Location location = locationDao.getLocationWithoutLockersAndCalendar(testLocation.getName());
        Assert.assertEquals("addLockersTest, added lockers", expectedLocation, location);

        // test adding negative amount of lockers
        int _n = -5;
        expectedLocation.setNumberOfLockers(prev_n + n + _n);
        locationDao.addLockers(testLocation.getName(), _n);
        location = locationDao.getLocationWithoutLockersAndCalendar(testLocation.getName());
        Assert.assertEquals("addLocker, added negative amount of lockers", expectedLocation, location);

        // TODO: reserve lockers and expect SQLException
    }

    @Test
    public void deleteLockersTest() {
        Location expectedLocation = testLocation.clone();
        int prev_n = expectedLocation.getNumberOfLockers();

        int n = 5;
        expectedLocation.setNumberOfLockers(prev_n - n);
        locationDao.deleteLockers(testLocation.getName(), prev_n - n);
        Location location = locationDao.getLocationWithoutLockersAndCalendar(testLocation.getName());
        Assert.assertEquals("deleteLockersTest", expectedLocation, location);
    }

    /*
    * getCalendarDays(), addCalendarDays() and deleteCalendarDays will be tested
    * */
    @Test
    public void calendarDaysTest() {
        Calendar calendar = TestSharedMethods.testCalendar();
        Collection<Day> calendarDays = calendar.getDays();

        locationDao.addCalendarDays(testLocation.getName(), calendar);

        Collection<Day> retrievedCalendarDays = locationDao.getCalendarDays(testLocation.getName());
        Assert.assertArrayEquals("calendarDaysTest, retrieved calendar days", calendarDays.toArray(), retrievedCalendarDays.toArray());

        locationDao.deleteCalendarDays(testLocation.getName(), "2020-01-01T00:00:00", "2020-01-05T00:00:00");
        retrievedCalendarDays = locationDao.getCalendarDays(testLocation.getName());
        Assert.assertArrayEquals("calendarDaysTest, deleted calendar days", new Day[]{}, retrievedCalendarDays.toArray());
    }
}
