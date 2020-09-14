package blok2.daos.db;

import blok2.daos.ITagsDao;
import blok2.helpers.Resources;
import blok2.model.LocationTag;
import blok2.model.reservables.Location;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DBTagsDao extends DAO implements ITagsDao {
    @Override
    public void addTag(LocationTag tag) throws SQLException {
        try (Connection conn = adb.getConnection()) {
            PreparedStatement st = conn.prepareStatement(Resources.databaseProperties.getString("add_tag"));
            st.setString(1, tag.getDutch());
            st.setString(2, tag.getEnglish());
            st.execute();
        }
    }

    public static void addTag(LocationTag tag, Connection conn) throws SQLException {
        PreparedStatement st = conn.prepareStatement(Resources.databaseProperties.getString("add_tag"));
        st.setString(1, tag.getDutch());
        st.setString(2, tag.getEnglish());
        st.execute();
    }

    @Override
    public void deleteTag(int tagId) throws SQLException {
        try (Connection conn = adb.getConnection()) {
            try {
                conn.setAutoCommit(false);

                // First, delete the entries in LOCATION_TAGS that have a reference to 'tagId'
                DBLocationTagDao.deleteTagFromAllLocations(tagId, conn);

                // Then, delete the tag
                deleteTagInTransaction(tagId, conn);

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void deleteTagInTransaction(int tagId, Connection conn) throws SQLException {
        PreparedStatement st = conn.prepareStatement(Resources.databaseProperties.getString("delete_tag"));
        st.setInt(1, tagId);
        st.execute();
    }

    @Override
    public void updateTag(LocationTag tag) throws SQLException {
        try (Connection conn = adb.getConnection()) {
            PreparedStatement st = conn.prepareStatement(Resources.databaseProperties.getString("update_tag"));
            st.setString(1, tag.getDutch());
            st.setString(2, tag.getEnglish());
            st.setInt(3, tag.getTagId());
            st.execute();
        }
    }

    @Override
    public ArrayList<LocationTag> getTags() throws SQLException {
        try (Connection conn = adb.getConnection()) {
            PreparedStatement st = conn.prepareStatement(Resources.databaseProperties.getString("all_tags"));
            ResultSet rs = st.executeQuery();
            return createLocationTagList(rs);
        }
    }

    @Override
    public LocationTag getTag(int tagId) throws SQLException {
        try (Connection conn = adb.getConnection()) {
            PreparedStatement st = conn.prepareStatement(Resources.databaseProperties.getString("get_tag"));
            st.setInt(1, tagId);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return createLocationTag(rs);
            }
            return null;
        }
    }

    /**
     * Assigning all LocationTags in the list "tags" to the location.
     * This is done by removing all tags from the location first, and then
     * adding the ones in the list.
     */
    @Override
    public void assignTagsToLocation(String locationName, List<LocationTag> tags) throws SQLException {
        try (Connection conn = adb.getConnection()) {
            try {
                conn.setAutoCommit(false);

                // remove all tags from the location
                DBLocationTagDao.deleteAllTagsFromLocation(locationName, conn);

                // add entries to connect the location with the tags
                for (LocationTag tag : tags) {
                    DBLocationTagDao.addTagToLocation(locationName, tag.getTagId(), conn);
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

    public static ArrayList<LocationTag> createLocationTagList(ResultSet rs) throws SQLException {
        ArrayList<LocationTag> tags = new ArrayList<>();
        while (rs.next()) {
            tags.add(createLocationTag(rs));
        }
        return tags;
    }

    public static LocationTag createLocationTag(ResultSet rs) throws SQLException {
        return new LocationTag(
                rs.getInt(Resources.databaseProperties.getString("tags_tag_id")),
                rs.getString(Resources.databaseProperties.getString("tags_dutch")),
                rs.getString(Resources.databaseProperties.getString("tags_english")));
    }
}
