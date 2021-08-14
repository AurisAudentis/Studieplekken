import { Component, OnInit, Input, TemplateRef } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { CalendarEvent } from 'angular-calendar';
import * as moment from 'moment';
import { Observable, Subject, BehaviorSubject, timer } from 'rxjs';
import { CalendarPeriodsService } from 'src/app/services/api/calendar-periods/calendar-periods.service';
import { LocationReservationsService } from 'src/app/services/api/location-reservations/location-reservations.service';
import {
  ApplicationTypeFunctionalityService
} from 'src/app/services/functionality/application-type/application-type-functionality.service';
import {
  CalendarPeriod
} from 'src/app/shared/model/CalendarPeriod';
import { LocationReservation } from 'src/app/shared/model/LocationReservation';
import { Timeslot } from 'src/app/shared/model/Timeslot';
import { LocationOpeningperiodDialogComponent } from './location-openingperiod-dialog/location-openingperiod-dialog.component';
import { Location } from 'src/app/shared/model/Location';
import { BsModalService } from 'ngx-bootstrap/modal';
import { AuthenticationService } from 'src/app/services/authentication/authentication.service';
import { Moment } from 'moment';
import { TranslateService } from '@ngx-translate/core';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { of } from 'rxjs/internal/observable/of';
import {
  ConversionToCalendarEventService
} from '../../../../services/styling/CalendarEvent/conversion-to-calendar-event.service';

@Component({
  selector: 'app-location-calendar',
  templateUrl: './location-calendar.component.html',
  styleUrls: ['./location-calendar.component.scss']
})
export class LocationCalendarComponent implements OnInit {
  @Input() location: Location; // only use this for creating a CalendarPeriod
  locationId: number; // will be set based on the url

  calendarPeriodsObs: Observable<CalendarPeriod[]>;
  errorSubject = new Subject<boolean>();

  locationReservations: LocationReservation[];
  currentTimeSlot: Timeslot;

  refresh: Subject<unknown> = new Subject<unknown>();

  prepareToUpdatePeriod: CalendarPeriod = null;
  currentCalendarPeriod: CalendarPeriod = null;

  calendarPeriodModel: BehaviorSubject<CalendarPeriod> = new BehaviorSubject(
    new CalendarPeriod(
      null,
      null,
      null,
      null,
      null,
      null,
      false,
      null,
      0,
      [],
      null,
      0
    )
  );

  /**
   * 'calendarPeriods' is the list of CalendarPeriods that the user
   * can modify using the form in the template
   */
  calendarPeriods: CalendarPeriod[] = [];

  /**
   * 'events' is the object that is used by the angular-calendar module
   * to show the events in the calendar.
   */
  events: CalendarEvent<{
    calendarPeriod: CalendarPeriod;
    timeslot?: Timeslot;
  }>[] = [];

  /**
   * 'eventsInDataLayer' is an object that keeps track of the opening
   * periods, that are stored in the data layer. This object is used
   * to be able to determine whether or not 'periods' has changed
   */
  calendarPeriodsInDataLayer: CalendarPeriod[] = [];

  disableFootButtons = true;

  showReservations = false;

  errorOnRetrievingReservations = false;

  successAddingLocationReservation: boolean = undefined;
  successUpdatingLocationReservation: boolean = undefined;
  successDeletingLocationReservation: boolean = undefined;

  /**
   * Depending on what the ApplicationTypeFunctionalityService returns
   * for the functionality of reservations, 'showReservationInformation'
   * will be set.
   */
  showReservationInformation: boolean;

  isAdmin: boolean = this.authenticationService.isAdmin();

  currentLang: string;

  constructor(
    private calendarPeriodsService: CalendarPeriodsService,
    private functionalityService: ApplicationTypeFunctionalityService,
    private locationReservationService: LocationReservationsService,
    private dialog: MatDialog,
    private modalService: MatDialog,
    private authenticationService: AuthenticationService,
    private translate: TranslateService,
    private route: ActivatedRoute,
    private conversionService: ConversionToCalendarEventService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.locationId = Number(this.route.snapshot.paramMap.get('locationId'));

    // Check if locationId is a Number before proceeding. If NaN, redirect to management locations.
    if (isNaN(this.locationId)) {
      this.router.navigate(['/management/locations']).catch(console.log);
      return;
    }

    this.currentLang = this.translate.currentLang;
    this.translate.onLangChange.subscribe(() => {
      this.currentLang = this.translate.currentLang;
      this.setup();
    });

    this.setup();

    this.showReservationInformation = this.functionalityService.showReservationsFunctionality();
  }

  // /**********
  // *   ADD   *
  // ***********/

  prepareAdd(template: TemplateRef<unknown>, el: HTMLElement): void {
    this.calendarPeriodModel.next(
      new CalendarPeriod(
        null,
        this.location,
        null,
        null,
        null,
        null,
        false,
        null,
        0,
        [],
        null,
        0
      )
    );
    this.prepareToUpdatePeriod = null;
    this.successAddingLocationReservation = undefined;
    el.scrollIntoView();
    this.modalService.open(template);
  }

  add(): void {
    this.update(true);
  }

  // /*************
  // *   UPDATE   *
  // **************/

  prepareUpdate(
    calendarPeriod: CalendarPeriod,
    template: TemplateRef<unknown>
  ): void {
    this.successUpdatingLocationReservation = undefined;
    this.prepareToUpdatePeriod = calendarPeriod;
    // Copy
    this.calendarPeriodModel.next(CalendarPeriod.fromJSON(calendarPeriod));
    this.modalService.open(template);
  }

  update(add = false): void {
    this.successAddingLocationReservation = null;
    this.successUpdatingLocationReservation = null;
    this.calendarPeriods = this.calendarPeriods.filter(
      (c) =>
        !this.prepareToUpdatePeriod || c.id !== this.prepareToUpdatePeriod.id
    );
    const updatedCalendarPeriod = this.calendarPeriodModel.value;
    console.log(updatedCalendarPeriod);
    if (this.calendarPeriodModel) {
      this.calendarPeriods = [
        ...this.calendarPeriods,
        this.calendarPeriodModel.value
      ];
    }

    // Check if the closing time - opening time is divisible by timeslot_size.
    // this.checkForWarning(this.calendarPeriods[this.calendarPeriods.length - 1]);

    // this.calendarPeriods is not empty, and all values are valid: persist update(s)
    this.calendarPeriodsService
      .updateCalendarPeriod(
        this.locationId,
        this.calendarPeriodsInDataLayer,
        this.calendarPeriodModel.value
      )
      .subscribe(
        () => {
          add
            ? (this.successAddingLocationReservation = true)
            : (this.successUpdatingLocationReservation = true);
          this.setup();
          this.modalService.closeAll();
        },
        () => {
          add
            ? (this.successAddingLocationReservation = false)
            : (this.successUpdatingLocationReservation = false);
          this.rollback();
        }
      );
  }

  // /*************
  // *   DELETE   *
  // **************/

  prepareDelete(
    calendarPeriod: CalendarPeriod,
    template: TemplateRef<unknown>
  ): void {
    this.prepareToUpdatePeriod = calendarPeriod;
    this.successDeletingLocationReservation = undefined;
    this.modalService.open(template);
  }

  delete(): void {
    this.successDeletingLocationReservation = null;
    this.calendarPeriodsService
      .deleteCalendarPeriods(this.prepareToUpdatePeriod)
      .subscribe(
        () => {
          this.successDeletingLocationReservation = true;
          this.setup();
        },
        () => {
          this.successDeletingLocationReservation = false;
          this.rollback();
        }
      );
  }

  // /******************
  // *   AUXILIARIES   *
  // *******************/

  setup(): void {
    // retrieve all calendar periods for this location
    this.calendarPeriodsObs = this.calendarPeriodsService
      .getCalendarPeriodsOfLocation(this.locationId)
      .pipe(
        tap((next) => {
          // Remark: due to references, 'this.calendarPeriods' has a reference to the same object
          // as the 'calendarPeriods' variable in the template through the assignation
          // 'calendarPeriodsObs | async as calendarPeriods'.
          this.calendarPeriods = next;

          // make a deep copy to make sure that can be calculated whether any period has changed
          this.calendarPeriodsInDataLayer = next.map(
            CalendarPeriod.fromJSON.bind(this)
          );

          // fill the events based on the calendar periods
          this.events = this.conversionService.mapCalendarPeriodsToCalendarEvents(
            next,
            this.translate.currentLang
          );

          // and update the calendar
          this.refresh.next(null);
        }),
        catchError((err) => {
          console.error(err);
          this.errorSubject.next(true);
          return of<CalendarPeriod[]>(null);
        })
      );
  }

  rollback(): void {
    this.calendarPeriods = [];
    this.calendarPeriodsInDataLayer.forEach((next) => {
      this.calendarPeriods.push(CalendarPeriod.fromJSON(next));
    });
  }

  checkForWarning(calendarPeriod: CalendarPeriod): void {
    let showWarning = false;

    const element = calendarPeriod;
    if (!element.reservable) {
      return;
    }

    // if the difference between closing time and opening time in minutes is
    // not divisible by the timeslot size (in minutes), then show the warning
    if (
      element.openingTime.diff(element.closingTime, 'minutes') %
        element.timeslotLength !==
      0
    ) {
      showWarning = true;
    }

    // if necessary, show the warning
    if (showWarning) {
      this.dialog.open(LocationOpeningperiodDialogComponent);
    }
  }

  timeslotPickedHandler(event: any): void {
    // event is a non-reservable calendar period.
    if (!event.timeslot) {
      this.showReservations = false;
      this.errorOnRetrievingReservations = false;
      return;
    }

    this.currentTimeSlot = event.timeslot as Timeslot;
    this.currentCalendarPeriod = event.calendarPeriod as CalendarPeriod;

    this.loadReservations();
  }

  loadReservations(): void {
    this.showReservations = null;
    timer(0, 60 * 1000)
      .pipe(
        switchMap(() =>
          this.locationReservationService.getLocationReservationsOfTimeslot(
            this.currentTimeSlot
          )
        )
      )
      .subscribe(
        (next) => {
          this.locationReservations = next;
          this.showReservations = true;
          this.errorOnRetrievingReservations = false;
        },
        () => {
          this.showReservations = false;
          this.errorOnRetrievingReservations = true;
        }
      );
  }

  getMinStartDate(): Moment {
    if (this.authenticationService.isAdmin()) {
      return null;
    } else {
      return moment().add(3, 'weeks').day(8);
    }
  }

  getMinReservableFrom(model: { startsAt: moment.MomentInput }): Moment {
    if (!model.startsAt) {
      return null;
    } else {
      return moment(model.startsAt).subtract(3, 'weeks').day(2);
    }
  }

  // If the admin is executing a change on own authority, show warning.
  showAdminWarnMessage(model: CalendarPeriod): boolean {
    if (!this.authenticationService.isAdmin()) {
      return false;
    }

    if (
      model.startsAt &&
      model.startsAt.isBefore(moment().add(3, 'weeks').day(8))
    ) {
      return true;
    }

    return (
      this.prepareToUpdatePeriod &&
      this.prepareToUpdatePeriod.startsAt.isBefore(
        moment().add(3, 'weeks').day(8)
      )
    );
  }

  closeModal(): void {
    this.modalService.closeAll();
  }

  getCalendarPeriodTimeInMinutes(
    calendarPeriod: CalendarPeriod
  ): number {
    if (!calendarPeriod.closingTime) { return null; }

    return calendarPeriod.openingTime?.diff(
      calendarPeriod.closingTime,
      'minutes'
    );
  }
}
