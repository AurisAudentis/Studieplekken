package be.ugent.blok2.daos;

import be.ugent.blok2.helpers.Institution;
import be.ugent.blok2.helpers.Resources;
import be.ugent.blok2.helpers.date.CustomDate;
import be.ugent.blok2.model.users.Role;
import be.ugent.blok2.model.users.User;
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

import java.util.ResourceBundle;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles({"db", "test"})
public class TestDBLocationDao {

    @Autowired
    private IAccountDao accountDao;

    @Autowired
    private ILocationDao locationDao;

    private final ResourceBundle applicationProperties = Resources.applicationProperties;

    private Location testLocation;
    private CustomDate startPeriodLockers;
    private CustomDate endPeriodLockers;

    private User scannerEmployee;
    private User scannerStudent;

    @Before
    public void setup() {
        // Change database credentials for used daos
        locationDao.setDatabaseConnectionUrl(applicationProperties.getString("test_db_url"));
        locationDao.setDatabaseCredentials(
                applicationProperties.getString("test_db_user"),
                applicationProperties.getString("test_db_password")
        );

        accountDao.setDatabaseConnectionUrl(applicationProperties.getString("test_db_url"));
        accountDao.setDatabaseCredentials(
                applicationProperties.getString("test_db_user"),
                applicationProperties.getString("test_db_password")
        );

        // setup test location object
        startPeriodLockers = new CustomDate(1970, 1, 1, 9, 0, 0);
        endPeriodLockers = new CustomDate(1970, 1, 31, 17, 0, 0);

        testLocation = new Location();
        testLocation.setName("Test Location");
        testLocation.setAddress("Test street, 10");
        testLocation.setNumberOfSeats(50);
        testLocation.setNumberOfLockers(15);
        testLocation.setMapsFrame("Test Google Maps frame");
        testLocation.setImageUrl("https://example.com/image.jpg");
        testLocation.setStartPeriodLockers(startPeriodLockers);
        testLocation.setEndPeriodLockers(endPeriodLockers);

        // setup test user objects
        scannerEmployee = new User();
        scannerEmployee.setFirstName("Scanner1");
        scannerEmployee.setLastName("Employee");
        scannerEmployee.setMail("scanner1.employee@ugent.be");
        scannerEmployee.setInstitution(Institution.UGent);
        scannerEmployee.setAugentID("003");
        scannerEmployee.setRoles(new Role[]{Role.EMPLOYEE});

        scannerStudent = new User();
        scannerStudent.setFirstName("Scanner2");
        scannerStudent.setLastName("Student");
        scannerStudent.setMail("scanner2.student@ugent.be");
        scannerStudent.setInstitution(Institution.UGent);
        scannerStudent.setAugentID("004");
        scannerStudent.setRoles(new Role[]{Role.STUDENT, Role.EMPLOYEE});
    }

    @After
    public void cleanup() {
        accountDao.useDefaultDatabaseConnection();
        locationDao.useDefaultDatabaseConnection();
    }

    @Test
    public void addLocationTest() {
        // add both user objects to the test database
        addTestUsers();

        locationDao.addLocation(testLocation);
        Location l = locationDao.getLocation(testLocation.getName());
        Assert.assertEquals("addLocation", testLocation, l);

        // remove both user objects from test database
        removeTestUsers();
    }

    private void addTestUsers() {
        accountDao.directlyAddUser(scannerEmployee);
        accountDao.directlyAddUser(scannerStudent);

        User u1 = accountDao.getUserById(scannerEmployee.getAugentID());
        Assert.assertEquals("Setup scannerEmployee failed", scannerEmployee, u1);

        User u2 = accountDao.getUserById(scannerStudent.getAugentID());
        Assert.assertEquals("Setup scannerStudent failed", scannerStudent, u2);
    }

    private void removeTestUsers() {
        accountDao.removeUserById(scannerEmployee.getAugentID());
        accountDao.removeUserById(scannerStudent.getAugentID());

        User u1 = accountDao.getUserById(scannerEmployee.getAugentID());
        Assert.assertNull("Cleanup scannerEmployee failed", u1);

        User u2 = accountDao.getUserById(scannerStudent.getAugentID());
        Assert.assertNull("Cleanup scannerStudent failed", u2);
    }
}
