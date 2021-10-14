package blok2.security.services;

import blok2.daos.IUserDao;
import blok2.model.users.User;
import blok2.security.config.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class JwtUserDetailService {

    private final IUserDao userDao;
    private final JwtService jwtService;

    @Autowired
    public JwtUserDetailService(IUserDao userDao, JwtService jwtService) {
        this.userDao = userDao;
        this.jwtService = jwtService;
    }

    public User loadUserDetails(String token) throws UsernameNotFoundException {
        String id = this.jwtService.getId(token);
        // Try to find the user with given id in the application database
        try {
            User user = this.userDao.getUserById(id);
            return user;
        } catch (Exception e) {
            return addUserToDb(token);

        }
    }

    public User addUserToDb(String token) {
        Claims claims = jwtService.getClaims(token);
        String id = claims.getSubject();
        String firstName = claims.get("fn", String.class);
        String lastName = claims.get("ln", String.class);
        String mail = claims.get("email", String.class);
        String institution = claims.get("ins", String.class);

        User user = new User();
        user.setUserId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setMail(mail);
        user.setInstitution(institution);
        userDao.addUser(user);
        return userDao.getUserById(id);
    }
}
