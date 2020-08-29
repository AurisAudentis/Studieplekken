package be.ugent.blok2.daos;

import be.ugent.blok2.model.Authority;
import be.ugent.blok2.model.users.User;

import java.sql.SQLException;
import java.util.List;

public interface IAuthorityDao extends IDao {

    // GETTERS
    /**
     * get all authorities
     */
    List<Authority> getAllAuthorities() throws SQLException;

    /**
     * get a list of Authorities the user is a member of. Can be empty.
     */
    List<Authority> getAuthoritiesFromUser(String augentId) throws SQLException;

    /**
     * get list of users that are a member of the given authority.
     */
    List<User> getUsersFromAuthority(int authorityId) throws SQLException;

    /**
     * get authority with the given name.
     */
    Authority getAuthorityByName(String name) throws SQLException;

    /**
     * get authority by its id.
     */
    Authority getAuthorityByAuthorityId(int authorityId) throws SQLException;

    /**
     * Add an authority to the database. AuthorityId is ignored.
     */
    void addAuthority(Authority authority) throws SQLException;

    /**
     *
     * Updates the authority given by the authorityId
     * @param updatedAuthority Authority with new values, with Authority.authorityId the authority to update
     */
    void updateAuthority(Authority updatedAuthority) throws SQLException;

}
