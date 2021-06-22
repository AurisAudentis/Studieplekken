package blok2.daos;

import blok2.BaseTest;
import blok2.TestSharedMethods;
import blok2.model.users.User;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;


public class TestDBAccountDao extends BaseTest {

    @Autowired
    private IAccountDao accountDao;

    private User testUser1;
    private User testUser2;

    @Override
    public void populateDatabase() throws SQLException {
        testUser1 = TestSharedMethods.adminTestUser();
        testUser2 = TestSharedMethods.studentTestUser();

        TestSharedMethods.addTestUsers(accountDao, testUser1, testUser2);
    }

    @Override
    public void cleanup() throws SQLException {
        TestSharedMethods.removeTestUsers(accountDao, testUser2, testUser1);
    }

    @Test
    public void directlyAddUserTest() throws SQLException {
        User directlyAddedUser = testUser1.clone();
        directlyAddedUser.setUserId("1" + testUser1.getUserId());
        directlyAddedUser.setMail("directly.addeduser@ugent.be");

        accountDao.directlyAddUser(directlyAddedUser);
        User u = accountDao.getUserById(directlyAddedUser.getUserId());
        Assert.assertEquals(directlyAddedUser, u);

        // remove added user
        TestSharedMethods.removeTestUsers(accountDao, directlyAddedUser);
    }

    @Test
    public void updateUserTest() throws SQLException {
        User expectedChangedUser = testUser1.clone();

        // change the role opposed to testUser1, update should succeed
        expectedChangedUser.setAdmin(false);

        accountDao.updateUserByMail(testUser1.getMail(), expectedChangedUser);

        User actualChangedUser = accountDao.getUserById(expectedChangedUser.getUserId());
        Assert.assertEquals(expectedChangedUser, actualChangedUser);
    }

    @Test(expected = SQLException.class)
    public void updateUserToExistingMailTest() throws SQLException {
        // change expectedChangedUser's mail to an existing mail, should fail
        User updated = testUser1.clone();
        updated.setMail(testUser2.getMail());
        accountDao.updateUserById(testUser1.getUserId(), updated);
    }

    @Test
    public void accountExistsByEmailTest() throws SQLException {
        boolean exists = accountDao.accountExistsByEmail(testUser1.getMail());
        Assert.assertTrue(exists);
    }

    @Test
    public void testGetters() throws SQLException {
        User u = accountDao.getUserByEmail(testUser1.getMail());
        Assert.assertEquals("getUserByEmail", testUser1, u);

        u = accountDao.getUserById(testUser1.getUserId());
        Assert.assertEquals("getUserById", testUser1, u);

        List<User> list = accountDao.getUsersByLastName(testUser1.getLastName());
        Assert.assertEquals("getUsersByLastName", 2, list.size());

        list = accountDao.getUsersByLastName("last_name_that_has_no_entry");
        Assert.assertEquals("getUsersByLastName" + list.size(), 0, list.size());

        list = accountDao.getUsersByFirstName(testUser1.getFirstName());
        Assert.assertEquals("getUsersByFirstName", 1, list.size());

        list = accountDao.getUsersByFirstName("first_name_that_has_no_entry");
        Assert.assertEquals("getUsersByFirstName", 0, list.size());

        list = accountDao.getUsersByFirstAndLastName(testUser1.getFirstName(), testUser1.getLastName());
        Assert.assertEquals("getUsersByFirstAndLastName", 1, list.size());
    }

    @Test
    public void getUserFromBarcodeTest() throws SQLException {
        // Code 128
        String barcode = testUser1.getUserId();
        User u = accountDao.getUserFromBarcode(barcode);
        Assert.assertEquals("getUserFromBarcodeTest, code 128", testUser1, u);

        // For the other codes, add another user
        User user = testUser1.clone();

        String user_student_number = "000140462060";
        String user_upca_barcode = "001404620603";
        String user_ean13_barcode = "0001404620603";
        String user_other_barcode = "0000140462060";

        user.setUserId(user_student_number);
        user.setMail("other_mail_due_to_unique_constraint@ugent.be");

        // Before every following assertion, the user is added, queried with the corresponding
        // encoded student number, and removed. The enclosing additions and removals are
        // included because if assertion fails, the state of the test database wouldn't be reset

        // UPC-A
        accountDao.directlyAddUser(user);
        u = accountDao.getUserFromBarcode(user_upca_barcode);
        accountDao.deleteUser(user.getUserId());
        Assert.assertEquals("getUserFromBarcodeTest, UPC-A", user, u);

        // EAN13
        accountDao.directlyAddUser(user);
        u = accountDao.getUserFromBarcode(user_ean13_barcode);
        accountDao.deleteUser(user.getUserId());
        Assert.assertEquals("getUserFromBarcodeTest, EAN13", user, u);

        // Other?
        accountDao.directlyAddUser(user);
        u = accountDao.getUserFromBarcode(user_other_barcode);
        accountDao.deleteUser(user.getUserId());
        Assert.assertEquals("getUserFromBarcodeTest, Other?", user, u);
    }
}
