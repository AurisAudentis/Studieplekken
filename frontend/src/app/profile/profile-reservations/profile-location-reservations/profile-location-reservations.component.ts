import {Component, OnInit} from '@angular/core';
import {AuthenticationService} from '../../../services/authentication/authentication.service';
import {Observable, Subscription} from 'rxjs';
import {LocationReservation} from '../../../shared/model/LocationReservation';
import {CalendarPeriodsService} from 'src/app/services/api/calendar-periods/calendar-periods.service';
import {CalendarPeriod} from 'src/app/shared/model/CalendarPeriod';
import {LocationReservationsService} from 'src/app/services/api/location-reservations/location-reservations.service';
import {Timeslot} from 'src/app/shared/model/Timeslot';
import {UserConstructor} from 'src/app/shared/model/User';

@Component({
  selector: 'app-profile-location-reservations',
  templateUrl: './profile-location-reservations.component.html',
  styleUrls: ['./profile-location-reservations.component.css']
})
export class ProfileLocationReservationsComponent implements OnInit {
  locationReservations: Observable<LocationReservation[]>;
  calendarMap: Map<number, CalendarPeriod> = new Map();
  calendarIdList: number[] = [];
  locationReservationToDelete: LocationReservation = new LocationReservation(UserConstructor.new(), null);
  locationReservationSub: Subscription;

  showReservations = false;
  loadingReservations = false;
  deletionWasSuccess: boolean = undefined;


  constructor(private authenticationService: AuthenticationService,
              private calendarPeriodService: CalendarPeriodsService,
              private locationReservationService: LocationReservationsService) {
    authenticationService.user.subscribe(next => {
    });
  }

  ngOnInit(): void {
    // only change the locationReservations if the user is logged in.
    // If you would omit the if-clause, a redudant API call will
    // be made with {userId} = '' (and thus requesting for all location
    // reservations stored in the database, this is not something
    // we want...
    if (this.authenticationService.isLoggedIn()) {
      this.locationReservations = this.authenticationService.getLocationReservations();
    }

    this.locationReservations.subscribe(next => {
      this.showReservations = false;
      this.loadingReservations = true;

      this.calendarIdList = [];
      next.forEach(element => {
        this.calendarIdList.push(element.timeslot.calendarId);
      });

      this.fillCalendarLocationMap();
    }, () => {
      this.showReservations = false;
      this.loadingReservations = false;
    });
  }

  fillCalendarLocationMap(): void {
    this.calendarPeriodService.getCalendarPeriods().subscribe(next => {
      next.forEach(element => {
        if (this.calendarIdList.includes(element.id)) {
          this.calendarMap.set(element.id, element);
        }
      });

      this.showReservations = true;
      this.loadingReservations = false;
    }, () => {
      this.showReservations = false;
      this.loadingReservations = false;
    });
  }

  prepareToDeleteLocationReservation(locationReservation: LocationReservation): void {
    this.deletionWasSuccess = undefined;
    this.locationReservationToDelete = locationReservation;
  }

  deleteLocationReservation(): void {
    this.locationReservationService.deleteLocationReservation(this.locationReservationToDelete).subscribe(
      () => {
        this.locationReservations = this.authenticationService.getLocationReservations();
        this.deletionWasSuccess = true;
      }, () => {
        this.locationReservations = this.authenticationService.getLocationReservations();
        this.deletionWasSuccess = false;
      }
    );
  }

  getBeginHour(calendarPeriod: CalendarPeriod, timeslot: Timeslot): Date {
    const d = new Date(calendarPeriod.startsAt + ' ' + calendarPeriod.openingTime);
    d.setTime(d.getTime() + timeslot.timeslotSeqnr * calendarPeriod.reservableTimeslotSize * 60000);
    return d;
  }
}
