package blok2.daos;

import blok2.helpers.date.CustomDate;
import blok2.model.Authority;
import blok2.model.reservables.Location;
import blok2.model.reservables.Locker;
import blok2.model.reservations.LockerReservation;
import blok2.model.users.User;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class TestDBLockerDao extends TestDao {

    @Autowired
    private ILocationDao locationDao;

    @Autowired
    private IAccountDao accountDao;

    @Autowired
    private ILockersDao lockersDao;

    @Autowired
    private ILockerReservationDao lockerReservationDao;

    @Autowired
    private IAuthorityDao authorityDao;

    private Location testLocation;
    private Authority authority;
    private User testUser;
    private List<Locker> testLockers;

    @Override
    public void populateDatabase() throws SQLException {
        // Setup test objects
        authority = TestSharedMethods.insertTestAuthority(authorityDao);
        testLocation = TestSharedMethods.testLocation(authority.getAuthorityId());
        testUser = TestSharedMethods.employeeAdminTestUser();
        testLockers = new ArrayList<>();

        // Add test object to database
        locationDao.addLocation(testLocation);
        TestSharedMethods.addTestUsers(accountDao, testUser);

        for (int i = 0; i < testLocation.getNumberOfLockers(); i++) {
            // Create test lockers
            Locker locker = new Locker();
            locker.setNumber(i);
            locker.setLocation(testLocation);

            testLockers.add(locker);
        }
    }

    @Test
    public void getLockerStatusesOfLocationTest() throws SQLException {
        /*
        Scenario: Reserve a single locker in the location and check that the method return
        NumberOfLockers - 1 results and that the reserved locker is not in the results. In that case the filter works.
         */
        // Get 3 random numbers of locker to reserve
        HashSet<Integer> set = new HashSet<>();
        Random r = new Random();
        while (set.size() < 3) {
            set.add(r.nextInt(testLocation.getNumberOfLockers()));
        }
        List<Integer> random = new ArrayList<>(set);

        // SCENARIO 1: reserve locker without pickup or returned date, key has not yet been picked up yet
        Locker l0 = testLockers.get(random.get(0));
        LockerReservation lr0 = new LockerReservation(l0, testUser);
        lockerReservationDao.addLockerReservation(lr0);

        // SCENARIO 2: reserve locker with pickup but without returned date, locker is still reserved.
        Locker l1 = testLockers.get(random.get(1));
        LockerReservation lr1 = new LockerReservation(l1, testUser);
        CustomDate pud1 = new CustomDate(1970, 1, 1, 9, 0, 0);
        lr1.setKeyPickupDate(pud1);
        lockerReservationDao.addLockerReservation(lr1);

        // SCENARIO 3: reserve locker with pickup and returned date, locker key has been returned
        Locker l2 = testLockers.get(random.get(2));
        LockerReservation lr2 = new LockerReservation(l2, testUser);
        CustomDate pud2 = new CustomDate(1970, 1, 1, 9, 0, 0);
        CustomDate rd2 = new CustomDate(1970, 1, 1, 9, 0, 1);
        lr2.setKeyPickupDate(pud2);
        lr2.setKeyReturnedDate(rd2);
        lockerReservationDao.addLockerReservation(lr2);

        // Retrieve the locker statuses
        List<LockerReservation> lockerStatusesOfLocation = lockersDao.getLockerStatusesOfLocation(testLocation.getName());

        // The first two scenarios should be included in the returned statuses, while the third scenario shouldn't.
        Assert.assertTrue("getLockerStatusesOfLocation, scenario 1", lockerStatusesOfLocation.contains(lr0));
        Assert.assertTrue("getLockerStatusesOfLocation, scenario 2", lockerStatusesOfLocation.contains(lr1));
        Assert.assertFalse("getLockerStatusesOfLocation, scenario 3", lockerStatusesOfLocation.contains(lr2));
    }
}
