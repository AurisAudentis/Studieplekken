import { Injectable } from '@angular/core';
import {BehaviorSubject, Observable} from 'rxjs';
import {User, UserConstructor} from '../../shared/model/User';
import {HttpClient} from '@angular/common/http';
import {api} from '../../../environments/environment';
import {Penalty} from '../../shared/model/Penalty';
import {LocationReservation} from '../../shared/model/LocationReservation';
import {LockerReservation, LockerReservationConstructor} from '../../shared/model/LockerReservation';
import {map, tap} from 'rxjs/operators';
import {Locker} from '../../shared/model/Locker';

/**
 * The structure of the authentication service has been based on this article:
 *   - https://jasonwatmore.com/post/2020/05/15/angular-9-role-based-authorization-tutorial-with-example
 *   - but without using the actual JWT authentication, this is not save as mentioned in:
 *     - https://dev.to/rdegges/please-stop-using-local-storage-1i04
 *
 * The authentication of non UGent users will use HTTP-only cookies created by the backend.
 * The authentication of UGent users will use a cookie created from CAS, HTTP-only as well.
 *
 * Importance of HTTP-only cookies: https://blog.codinghorror.com/protecting-your-cookies-httponly/
 */
@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {
  // BehaviorSubject to be able to emit on changes
  // private so that only the AuthenticationService can modify the user
  private userSubject: BehaviorSubject<User> = new BehaviorSubject<User>(UserConstructor.new());
  // and other components can subscribe using the public observable
  // (which comes from the userSubject)
  public user: Observable<User> = this.userSubject.asObservable();

  constructor(private http: HttpClient) {
    // TODO: try to obtain a user object based on a HTTP-only session cookie, if provided
    //   this way, if a user was logged in previously, he/she doesn't have to do it again
    http.get<User>(api.user_by_mail.replace('{mail}', 'bram.vandewalle@ugent.be'))
      .subscribe(next => {
        this.userSubject.next(next);
    });
  }

  userValue(): User {
    return this.userSubject.value;
  }

  login(mail: string, password: string): void {
    // TODO: login
  }

  logout(): void {
    // TODO: logout
  }

  isLoggedIn(): boolean {
    return this.userSubject.value.augentID !== '';
  }

  updatePassword(user: User): void {
    // TODO: update password
  }

  getLocationReservations(): Observable<LocationReservation[]> {
    return this.http.get<LocationReservation[]>(api.locationReservationsByUserId.replace('{userId}',
      this.userSubject.value.augentID));
  }

  getLockerReservations(): Observable<LockerReservation[]> {
    const v = this.http.get<LockerReservation[]>(api.lockerReservationsByUserId.replace('{userId}',
      this.userSubject.value.augentID));

    return v.pipe(map<LockerReservation[], LockerReservation[]>((value, index) => {
      const reservations: LockerReservation[] = [];

      value.forEach(reservation => {
        const obj = LockerReservationConstructor.newFromObj(reservation);
        reservations.push(obj);
      });

      return reservations;
    }));
  }

  getPenalties(): Observable<Penalty[]> {
    return this.http.get<Penalty[]>(api.penalties_by_user_id.replace('{userId}',
      this.userSubject.value.augentID));
  }
}
