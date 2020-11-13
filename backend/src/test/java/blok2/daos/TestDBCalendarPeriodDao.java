package blok2.daos;

import blok2.helpers.LocationStatus;
import blok2.helpers.Pair;
import blok2.model.Authority;
import blok2.model.Building;
import blok2.model.calendar.CalendarPeriod;
import blok2.model.reservables.Location;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TestDBCalendarPeriodDao extends TestDao {

    @Autowired
    private ICalendarPeriodDao calendarPeriodDao;

    @Autowired
    private ILocationDao locationDao;

    @Autowired
    private IAuthorityDao authorityDao;

    @Autowired
    private IBuildingDao buildingDao;

    private Location testLocation;
    private Building testBuilding;

    // the reason for making this an attribute of the class
    // is to make sure the values are deleted when something
    // goes wrong
    private List<CalendarPeriod> calendarPeriods;
    private List<CalendarPeriod> updatedPeriods;
    private CalendarPeriod pastPeriod;
    private CalendarPeriod upcomingPeriod;
    private CalendarPeriod activePeriodOutsideHours;
    private CalendarPeriod activePeriodInsideHours;

    @Override
    public void populateDatabase() throws SQLException {
        // Setup test objects
        Authority authority = TestSharedMethods.insertTestAuthority(authorityDao);

        testBuilding = buildingDao.addBuilding(TestSharedMethods.testBuilding());

        testLocation = TestSharedMethods.testLocation(authority.clone(), testBuilding);
        calendarPeriods = TestSharedMethods.testCalendarPeriods(testLocation);
        updatedPeriods = TestSharedMethods.testCalendarPeriodsButUpdated(testLocation);
        pastPeriod = TestSharedMethods.pastCalendarPeriods(testLocation);
        upcomingPeriod = TestSharedMethods.upcomingCalendarPeriods(testLocation);
        activePeriodOutsideHours = TestSharedMethods.activeCalendarPeriodsOutsideHours(testLocation);
        activePeriodInsideHours = TestSharedMethods.activeCalendarPeriodsInsideHours(testLocation);

        // Add test objects to database
        locationDao.addLocation(testLocation);
    }

    @Test
    public void addCalendarPeriodsTest() throws SQLException {
        // Add calendar periods to database
        calendarPeriodDao.addCalendarPeriods(calendarPeriods);

        // Check if the addition worked properly
        List<CalendarPeriod> actualPeriods = calendarPeriodDao.getCalendarPeriodsOfLocation(testLocation.getName());
        actualPeriods.sort(Comparator.comparing(CalendarPeriod::toString));
        calendarPeriods.sort(Comparator.comparing(CalendarPeriod::toString));

        Assert.assertEquals("addCalendarPeriodsTest", calendarPeriods, actualPeriods);
    }

    @Test
    public void getStatusTest() throws SQLException {
        // First, add only past calendar periods
        List<CalendarPeriod> pastPeriods = new ArrayList<>();
        pastPeriods.add(pastPeriod);
        calendarPeriodDao.addCalendarPeriods(pastPeriods);

        Assert.assertEquals("StatusTest, only past calendar periods",
                new Pair<>(LocationStatus.CLOSED, ""),
                calendarPeriodDao.getStatus(testLocation.getName())
        );

        // Second, add upcoming calendar periods. The past periods may remain, these do not affect status
        List<CalendarPeriod> upcomingPeriods = new ArrayList<>();
        upcomingPeriods.add(upcomingPeriod);
        calendarPeriodDao.addCalendarPeriods(upcomingPeriods);

        Pair<LocationStatus, String> expectedStatus = new Pair<>(LocationStatus.CLOSED_UPCOMING, upcomingPeriod.getStartsAt() + " " + upcomingPeriod.getOpeningTime());
        Pair<LocationStatus, String> retrievedStatus = calendarPeriodDao.getStatus(testLocation.getName());
        Assert.assertEquals("StatusTest, past and upcoming calendar periods",
                expectedStatus,
                retrievedStatus
        );

        // Third, add active periods.
        // There are two cases here: the current time is within or outside the opening hours.
        // These cases will be handled seperately

        // First: outside hours
        List<CalendarPeriod> outsideHours = new ArrayList<>();
        outsideHours.add(activePeriodOutsideHours);
        calendarPeriodDao.addCalendarPeriods(outsideHours);

        Assert.assertEquals("StatusTest, active period, outside hours",
                new Pair<>(LocationStatus.CLOSED_ACTIVE, activePeriodOutsideHours.getStartsAt() + " " + activePeriodOutsideHours.getOpeningTime()),
                calendarPeriodDao.getStatus(testLocation.getName())
        );

        // Before the case of active period inside the hours, remove outside hours
        calendarPeriodDao.deleteCalendarPeriods(outsideHours);

        // Second: inside hours
        List<CalendarPeriod> insideHours = new ArrayList<>();
        insideHours.add(activePeriodInsideHours);
        calendarPeriodDao.addCalendarPeriods(insideHours);

        Assert.assertEquals("StatusTest, active period, inside hours",
                new Pair<>(LocationStatus.OPEN, activePeriodInsideHours.getEndsAt() + " " + activePeriodInsideHours.getClosingTime()),
                calendarPeriodDao.getStatus(testLocation.getName())
        );

    }

    @Test
    public void updateCalendarPeriodsTest() throws SQLException {
        // Add calendar periods to database
        calendarPeriodDao.addCalendarPeriods(calendarPeriods);

        // update the periods
        calendarPeriodDao.updateCalendarPeriods(calendarPeriods, updatedPeriods);

        // check whether the periods are successfully updated
        List<CalendarPeriod> actualPeriods = calendarPeriodDao.getCalendarPeriodsOfLocation(testLocation.getName());
        actualPeriods.sort(Comparator.comparing(CalendarPeriod::toString));
        updatedPeriods.sort(Comparator.comparing(CalendarPeriod::toString));
        Assert.assertEquals("updateCalendarPeriodsTest", updatedPeriods, actualPeriods);
    }

    @Test
    public void deleteCalendarPeriodsTest() throws SQLException {
        // Add calendar periods to database
        calendarPeriodDao.addCalendarPeriods(calendarPeriods);

        // Delete the calendar periods from the database
        calendarPeriodDao.deleteCalendarPeriods(calendarPeriods);

        // are the periods deleted?
        List<CalendarPeriod> actualPeriods = calendarPeriodDao.getCalendarPeriodsOfLocation(testLocation.getName());
        Assert.assertEquals("deleteCalendarPeriodsTest", 0, actualPeriods.size());
    }
}
