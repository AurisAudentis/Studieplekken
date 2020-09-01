package blok2.daos.cascade;

import blok2.daos.TestSharedMethods;
import blok2.daos.*;
import blok2.helpers.Language;
import blok2.helpers.date.CustomDate;
import blok2.model.penalty.Penalty;
import blok2.model.penalty.PenaltyEvent;
import blok2.model.reservables.Location;
import blok2.model.reservables.Locker;
import blok2.model.reservations.LocationReservation;
import blok2.model.reservations.LockerReservation;
import blok2.model.users.Role;
import blok2.model.users.User;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.SQLException;
import java.util.*;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles({"db", "test"})
public class TestCascadeInDBAccountDao {

    @Autowired
    private IAccountDao accountDao;

    @Autowired
    private ILocationDao locationDao;

    @Autowired
    private ILocationReservationDao locationReservationDao;

    @Autowired
    private ILockerReservationDao lockerReservationDao;

    @Autowired
    private IPenaltyEventsDao penaltyEventsDao;

    @Autowired
    private IScannerLocationDao scannerLocationDao;

    // this will be the test user
    private User testUser;

    // for cascade on SCANNERS_LOCATION, LOCATION_RESERVATIONS
    // and LOCKER_RESERVATIONS, a Location must be available
    private Location testLocation1;
    private Location testLocation2;

    // to test cascade on LOCATION_RESERVATIONS
    private LocationReservation testLocationReservation1;
    private LocationReservation testLocationReservation2;

    // to test cascade on LOCKER_RESERVATIONS
    private LockerReservation testLockerReservation1;
    private LockerReservation testLockerReservation2;

    // to test cascade on PENALTY_BOOK
    private PenaltyEvent testPenaltyEvent;
    private Penalty testPenalty1;
    private Penalty testPenalty2;

    @Before
    public void setup() throws SQLException {
        // Use test database
        TestSharedMethods.setupTestDaoDatabaseCredentials(accountDao);
        TestSharedMethods.setupTestDaoDatabaseCredentials(locationDao);
        TestSharedMethods.setupTestDaoDatabaseCredentials(locationReservationDao);
        TestSharedMethods.setupTestDaoDatabaseCredentials(lockerReservationDao);
        TestSharedMethods.setupTestDaoDatabaseCredentials(penaltyEventsDao);
        TestSharedMethods.setupTestDaoDatabaseCredentials(scannerLocationDao);

        // Setup test objects
        testUser = TestSharedMethods.studentEmployeeTestUser();
        testLocation1 = TestSharedMethods.testLocation();
        testLocation2 = TestSharedMethods.testLocation2();

        testLocationReservation1 = new LocationReservation(testLocation1, testUser, CustomDate.now());
        testLocationReservation2 = new LocationReservation(testLocation2, testUser, new CustomDate(1970, 1, 1));

        Locker testLocker1 = new Locker(0, testLocation1);
        Locker testLocker2 = new Locker(0, testLocation2);

        testLockerReservation1 = new LockerReservation(testLocker1, testUser);
        testLockerReservation2 = new LockerReservation(testLocker2, testUser);

        Map<Language, String> descriptions = new HashMap<>();
        descriptions.put(Language.DUTCH, "Dit is een test omschrijving van een penalty event met code 0");
        descriptions.put(Language.ENGLISH, "This is a test description of a penalty event with code 0");
        testPenaltyEvent = new PenaltyEvent(0, 10, descriptions);

        // Note: the received amount of points are 10 and 20, not testPenaltyEvent.getCode()
        // because when the penalties are retrieved from the penaltyEventDao, the list will
        // be sorted by received points before asserting, if they would be equal we can't sort
        // on the points and be sure about the equality of the actual and expected list.
        testPenalty1 = new Penalty(testUser.getAugentID(), testPenaltyEvent.getCode(), CustomDate.now(), CustomDate.now(), testLocation1.getName(), 10, "First test penalty");
        testPenalty2 = new Penalty(testUser.getAugentID(), testPenaltyEvent.getCode(), new CustomDate(1970, 1, 1), CustomDate.now(), testLocation2.getName(), 20, "Second test penalty");

        // Add test objects to database
        accountDao.directlyAddUser(testUser);

        locationDao.addLocation(testLocation1);
        locationDao.addLocation(testLocation2);

        locationReservationDao.addLocationReservation(testLocationReservation1);
        locationReservationDao.addLocationReservation(testLocationReservation2);

        lockerReservationDao.addLockerReservation(testLockerReservation1);
        lockerReservationDao.addLockerReservation(testLockerReservation2);

        penaltyEventsDao.addPenaltyEvent(testPenaltyEvent);
        penaltyEventsDao.addPenalty(testPenalty1);
        penaltyEventsDao.addPenalty(testPenalty2);

        scannerLocationDao.addScannerLocation(testLocation1.getName(), testUser.getAugentID());
        scannerLocationDao.addScannerLocation(testLocation2.getName(), testUser.getAugentID());
    }

    @After
    public void cleanup() throws SQLException {
        // Remove test objects from database
        // Note, I am not relying on the cascade because that's
        // what we are testing here in this class ...
        scannerLocationDao.deleteAllLocationsOfScanner(testUser.getAugentID());

        penaltyEventsDao.deletePenalty(testPenalty2);
        penaltyEventsDao.deletePenalty(testPenalty1);
        penaltyEventsDao.deletePenaltyEvent(testPenaltyEvent.getCode());

        lockerReservationDao.deleteLockerReservation(testLockerReservation2.getLocker().getLocation().getName(),
                testLockerReservation2.getLocker().getNumber());
        lockerReservationDao.deleteLockerReservation(testLockerReservation1.getLocker().getLocation().getName(),
                testLockerReservation1.getLocker().getNumber());

        locationReservationDao.deleteLocationReservation(testLocationReservation2.getUser().getAugentID(),
                testLocationReservation2.getDate());
        locationReservationDao.deleteLocationReservation(testLocationReservation1.getUser().getAugentID(),
                testLocationReservation1.getDate());

        // ... okay, cascade is assumed to be okay for the lockers here... (but it is)
        locationDao.deleteLocation(testLocation2.getName());
        locationDao.deleteLocation(testLocation1.getName());

        accountDao.deleteUser(testUser.getAugentID());

        // Use regular database
        accountDao.useDefaultDatabaseConnection();
        locationDao.useDefaultDatabaseConnection();
        locationReservationDao.useDefaultDatabaseConnection();
        lockerReservationDao.useDefaultDatabaseConnection();
        penaltyEventsDao.useDefaultDatabaseConnection();
        scannerLocationDao.useDefaultDatabaseConnection();
    }

    @Test
    public void updateUserWithoutCascadeNeededTest() throws SQLException {
        updateUserFieldWithoutAUGentID(testUser);
        accountDao.updateUserById(testUser.getAugentID(), testUser);
        User u = accountDao.getUserById(testUser.getAugentID());
        Assert.assertEquals("updateUserWithoutCascadeNeededTest", testUser, u);

        LocationReservation lr1 = locationReservationDao.getLocationReservation(
                testLocationReservation1.getUser().getAugentID(),
                testLocationReservation1.getDate());
        Assert.assertEquals("updateUserWithoutCascadeNeededTest, testLocationReservation1",
                testLocationReservation1, lr1);

        LocationReservation lr2 = locationReservationDao.getLocationReservation(
                testLocationReservation2.getUser().getAugentID(),
                testLocationReservation2.getDate());
        Assert.assertEquals("updateUserWithoutCascadeNeededTest, testLocationReservation2",
                testLocationReservation2, lr2);

        LockerReservation lor1 = lockerReservationDao.getLockerReservation(
                testLockerReservation1.getLocker().getLocation().getName(),
                testLockerReservation1.getLocker().getNumber());
        Assert.assertEquals("updateUserWithoutCascadeNeededTest, testLockerReservation1",
                testLockerReservation1, lor1);

        LockerReservation lor2 = lockerReservationDao.getLockerReservation(
                testLockerReservation2.getLocker().getLocation().getName(),
                testLockerReservation2.getLocker().getNumber());
        Assert.assertEquals("updateUserWithoutCascadeNeededTest, testLockerReservation2",
                testLockerReservation2, lor2);

        List<Penalty> penalties = penaltyEventsDao.getPenaltiesByUser(testUser.getAugentID());
        penalties.sort(Comparator.comparing(Penalty::getReceivedPoints));

        List<Penalty> expectedPenalties = new ArrayList<>();
        expectedPenalties.add(testPenalty1);
        expectedPenalties.add(testPenalty2);
        expectedPenalties.sort(Comparator.comparing(Penalty::getReceivedPoints));

        Assert.assertEquals("updateUserWithoutCascadeNeededTest, penalties", expectedPenalties,
                penalties);

        List<Location> scannerLocations = scannerLocationDao.getLocationsToScanOfUser(testUser.getAugentID());
        scannerLocations.sort(Comparator.comparing(Location::getName));

        List<Location> expectedLocations = new ArrayList<>();
        expectedLocations.add(testLocation1);
        expectedLocations.add(testLocation2);
        expectedLocations.sort(Comparator.comparing(Location::getName));

        Assert.assertEquals("updateUserWithoutCascadeNeededTest, locations to scan with new id",
                expectedLocations, scannerLocations);
    }

    @Test
    public void updateUserWithCascadeNeededTest() throws SQLException {
        updateUserFieldWithoutAUGentID(testUser);
        String oldAUGentID = testUser.getAugentID();
        testUser.setAugentID(testUser.getAugentID() + "Iets in een test");
        accountDao.updateUserById(oldAUGentID, testUser);

        // the User needs to be updated in the first place
        User u = accountDao.getUserById(testUser.getAugentID());
        Assert.assertEquals("updateUserWithCascadeNeededTest, user", testUser, u);

        // the User with the old augentid needs to be removed
        u = accountDao.getUserById(oldAUGentID);
        Assert.assertNull("updateUserWithCascadeNeededTest, old user needs to be removed", u);

        // check whether the entries in LOCATION_RESERVATIONS have been updated in cascade
        // note that because of references, the User object in testLocationReservation1/2
        // have the updated testUser
        LocationReservation lr1 = locationReservationDao.getLocationReservation(
                testLocationReservation1.getUser().getAugentID(),
                testLocationReservation1.getDate());
        Assert.assertEquals("updateUserWithCascadeNeededTest, testLocationReservation1",
                testLocationReservation1, lr1);

        LocationReservation lr2 = locationReservationDao.getLocationReservation(
                testLocationReservation2.getUser().getAugentID(),
                testLocationReservation2.getDate());
        Assert.assertEquals("updateUserWithCascadeNeededTest, testLocationReservation2",
                testLocationReservation2, lr2);

        // check whether the entries in LOCKER_RESERVATIONS have been updated in cascade
        LockerReservation lor1 = lockerReservationDao.getLockerReservation(
                testLockerReservation1.getLocker().getLocation().getName(),
                testLockerReservation1.getLocker().getNumber());
        Assert.assertEquals("updateUserWithCascadeNeededTest, testLockerReservation1",
                testLockerReservation1, lor1);

        LockerReservation lor2 = lockerReservationDao.getLockerReservation(
                testLockerReservation2.getLocker().getLocation().getName(),
                testLockerReservation2.getLocker().getNumber());
        Assert.assertEquals("updateUserWithCascadeNeededTest, testLockerReservation2",
                testLockerReservation2, lor2);

        // check whether the entries in PENALTY_BOOK have been updated in cascade
        List<Penalty> penalties = penaltyEventsDao.getPenaltiesByUser(testUser.getAugentID());
        penalties.sort(Comparator.comparing(Penalty::getReceivedPoints));

        // Penalty objects don't keep a reference to User, but have a String with the augentid
        testPenalty1.setAugentID(testUser.getAugentID());
        testPenalty2.setAugentID(testUser.getAugentID());

        List<Penalty> expectedPenalties = new ArrayList<>();
        expectedPenalties.add(testPenalty1);
        expectedPenalties.add(testPenalty2);
        expectedPenalties.sort(Comparator.comparing(Penalty::getReceivedPoints));

        Assert.assertEquals("updateUserWithCascadeNeededTest, penalties", expectedPenalties,
                penalties);

        // check whether the entries in SCANNERS_LOCATION have been updated in cascade
        List<Location> scannerLocations = scannerLocationDao.getLocationsToScanOfUser(oldAUGentID);
        Assert.assertEquals("updateUserWithCascadeNeededTest, locations to scan with old id",
                0, scannerLocations.size());

        scannerLocations = scannerLocationDao.getLocationsToScanOfUser(testUser.getAugentID());
        scannerLocations.sort(Comparator.comparing(Location::getName));

        List<Location> expectedLocations = new ArrayList<>();
        expectedLocations.add(testLocation1);
        expectedLocations.add(testLocation2);
        expectedLocations.sort(Comparator.comparing(Location::getName));

        Assert.assertEquals("updateUserWithCascadeNeededTest, locations to scan with new id",
                expectedLocations, scannerLocations);
    }

    @Test
    public void deleteUserTest() throws SQLException {
        accountDao.deleteUser(testUser.getAugentID());
        User u = accountDao.getUserById(testUser.getAugentID());
        Assert.assertNull("deleteUserTest, user must be deleted", u);

        List<Location> scannerLocations = scannerLocationDao.getLocationsToScanOfUser(testUser.getAugentID());
        Assert.assertEquals("deleteUserTest, scannerLocations", 0,
                scannerLocations.size());

        List<Penalty> penalties = penaltyEventsDao.getPenaltiesByUser(testUser.getAugentID());
        Assert.assertEquals("deleteUserTest, penalties", 0, penalties.size());

        List<LocationReservation> locationReservations = locationReservationDao
                .getAllLocationReservationsOfUser(testUser.getAugentID());
        Assert.assertEquals("deleteUserTest, location reservations", 0,
                locationReservations.size());

        List<LockerReservation> lockerReservations = lockerReservationDao
                .getAllLockerReservationsOfUser(testUser.getAugentID());
        Assert.assertEquals("deleteUserTest, locker reservations", 0,
                lockerReservations.size());
    }

    private void updateUserFieldWithoutAUGentID(User user) {
        user.setLastName("Changed last name");
        user.setFirstName("Changed first name");
        user.setMail("Changed.Mail@UGent.be");
        user.setPassword("Changed Password");
        user.setInstitution("UGent");
        user.setRoles(new Role[]{Role.STUDENT});
    }
}