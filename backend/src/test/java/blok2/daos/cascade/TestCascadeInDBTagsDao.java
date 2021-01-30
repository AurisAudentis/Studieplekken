package blok2.daos.cascade;

import blok2.TestSharedMethods;
import blok2.daos.*;
import blok2.model.Authority;
import blok2.model.Building;
import blok2.model.LocationTag;
import blok2.model.reservables.Location;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;

public class TestCascadeInDBTagsDao extends TestDao {

    @Autowired
    private ILocationDao locationDao;

    @Autowired
    private ITagsDao tagsDao;

    @Autowired
    private ILocationTagDao locationTagDao;

    @Autowired
    private IAuthorityDao authorityDao;

    @Autowired
    private IBuildingDao buildingDao;

    // these will be the test locations
    private Location testLocation1;
    private Location testLocation2;
    private Location testLocation3;

    // these will be used as testtags
    private LocationTag testTag;
    private LocationTag testTag2;

    @Override
    public void populateDatabase() throws SQLException {
        // setup test objects
        Authority authority = TestSharedMethods.insertTestAuthority(authorityDao);
        Building testBuilding = buildingDao.addBuilding(TestSharedMethods.testBuilding());
        testLocation1 = TestSharedMethods.testLocation(authority.clone(), testBuilding);
        testLocation2 = TestSharedMethods.testLocation2(authority.clone(), testBuilding);
        testLocation3 = TestSharedMethods.testLocation3(authority.clone(), testBuilding);

        testTag = TestSharedMethods.testTag();
        testTag2 = TestSharedMethods.testTag2();

        // add test objects to the database
        locationDao.addLocation(testLocation1);
        locationDao.addLocation(testLocation2);
        locationDao.addLocation(testLocation3);

        tagsDao.addTag(testTag);
        tagsDao.addTag(testTag2);
    }

    @Test
    public void deleteLocationTagWithCascadeNeeded() throws SQLException {
        // first add the entries to LOCATION_TAGS
        locationTagDao.addTagToLocation(testLocation1.getName(), testTag.getTagId());
        locationTagDao.addTagToLocation(testLocation2.getName(), testTag.getTagId());
        locationTagDao.addTagToLocation(testLocation2.getName(), testTag2.getTagId());

        // Assert that the assignment of tags to the location properly worked
        Assert.assertTrue("deleteLocationTagWithCascadeNeeded, add location tag", locationDao.getLocation(testLocation1.getName()).getAssignedTags().contains(testTag));
        Assert.assertFalse("deleteLocationTagWithCascadeNeeded, add location tag", locationDao.getLocation(testLocation1.getName()).getAssignedTags().contains(testTag2));
        Assert.assertTrue("deleteLocationTagWithCascadeNeeded, add location tag", locationDao.getLocation(testLocation2.getName()).getAssignedTags().contains(testTag));
        Assert.assertTrue("deleteLocationTagWithCascadeNeeded, add location tag", locationDao.getLocation(testLocation2.getName()).getAssignedTags().contains(testTag2));
        Assert.assertFalse("deleteLocationTagWithCascadeNeeded, add location tag", locationDao.getLocation(testLocation3.getName()).getAssignedTags().contains(testTag));
        Assert.assertFalse("deleteLocationTagWithCascadeNeeded, add location tag", locationDao.getLocation(testLocation3.getName()).getAssignedTags().contains(testTag2));

        // Remove tag 1
        tagsDao.deleteTag(testTag.getTagId());

        // Assert that the deletion of the tags cascaded to the locations
        Assert.assertFalse("deleteLocationTagWithCascadeNeeded, cascade delete location tag", locationDao.getLocation(testLocation1.getName()).getAssignedTags().contains(testTag));
        Assert.assertFalse("deleteLocationTagWithCascadeNeeded, cascade delete location tag", locationDao.getLocation(testLocation2.getName()).getAssignedTags().contains(testTag));
        Assert.assertTrue("deleteLocationTagWithCascadeNeeded, cascade delete location tag", locationDao.getLocation(testLocation2.getName()).getAssignedTags().contains(testTag2));
    }
}
