import {Component, Input, OnInit} from '@angular/core';
import {Observable, Subject} from 'rxjs';
import {Location} from '../../../../shared/model/Location';
import {CalendarEvent} from 'angular-calendar';
import {CalendarPeriodsForLockersService} from '../../../../services/api/calendar-periods-for-lockers/calendar-periods-for-lockers.service';
import {
  CalendarPeriodForLockers,
  CalendarPeriodForLockersConstructor, calendarPeriodForLockersToCalendarEvent, isCalendarPeriodForLockersValid
} from '../../../../shared/model/CalendarPeriodForLockers';
import {equalCalendarPeriodsForLockers} from '../../../../shared/comparators/ModelComparators';

@Component({
  selector: 'app-lockers-calendar',
  templateUrl: './lockers-calendar.component.html',
  styleUrls: ['./lockers-calendar.component.css']
})
export class LockersCalendarComponent implements OnInit {
  @Input() location: Observable<Location>;

  refresh: Subject<any> = new Subject();

  /*
   * 'events' is the object that is used by the frontend to update/add periods.
   */
  events: CalendarEvent<CalendarPeriodForLockers>[] = [];

  /*
   * 'eventsInDataLayer' is an object that keeps track of the opening
   * periods that are stored in the data layer. This object is used
   * to be able to get the updated/added periods, so that the backend
   * can determine what objects to add/delete/update
   */
  calendarPeriodsForLockersInDataLayer: CalendarPeriodForLockers[] = [];

  disableFootButtons = true;

  /*
   * The boolean-attributes below are used to give feedback to the
   * user when he/she has pressed the "Update" button in different
   * scenarios.
   */
  msToShowFeedback = 10000; // 10 sec
  showWrongCalendarPeriodForLockersFormat = false;
  showSuccessButNoChanges = false;
  showSuccess = false;
  showError = false;

  constructor(private calendarPeriodsForLockersService: CalendarPeriodsForLockersService) { }

  ngOnInit(): void {
    this.location.subscribe(next => {
      this.setupEvents(next.name);
    });
  }

  setupEvents(locationName: string): void {
    // retrieve all calendar periods for the lockers of this location
    this.calendarPeriodsForLockersService.getCalendarPeriodsForLockersOfLocation(locationName).subscribe(next => {
      if (next === null) {
        return;
      }

      // make a deep copy to make sure that we can calculate whether or not any changes by the user have been made
      this.calendarPeriodsForLockersInDataLayer = [];
      next.forEach(n => {
        this.calendarPeriodsForLockersInDataLayer.push(CalendarPeriodForLockersConstructor.newFromObj(n));
      });

      // fill the events based on the calendar periods for lockers
      this.events = this.mapCalendarPeriodsForLockersToCalendarEvents(next);
    });
  }

  mapCalendarPeriodsForLockersToCalendarEvents(periods: CalendarPeriodForLockers[]): CalendarEvent[] {
    return periods.map<CalendarEvent>(n => {
      return calendarPeriodForLockersToCalendarEvent(n);
    });
  }

  isPeriodInEvents(period: CalendarPeriodForLockers): boolean {
    return this.events.findIndex(n => equalCalendarPeriodsForLockers(period, n.meta)) < 0;
  }

  isPeriodInBackend(event: CalendarEvent): boolean {
    return this.calendarPeriodsForLockersInDataLayer.findIndex(
      n => equalCalendarPeriodsForLockers(n, event.meta
      )) < 0;
  }

  hasAnyPeriodChanged(): boolean {
    // if the lengths do not match, there must have changed something
    if (this.events.length !== this.calendarPeriodsForLockersInDataLayer.length) {
      return true;
    }

    // if the lengths do match, try to find all values in this.calendarPeriodsForLockersInDataLayer
    // in this.events
    this.calendarPeriodsForLockersInDataLayer.forEach(n => {
      if (!this.isPeriodInEvents(n)) {
        return false;
      }
    });

    return true;
  }

  refreshCalendar(event: CalendarEvent): void {
    // find index corresponding to the given event
    const idx = this.events.findIndex(n => event === n);

    if (idx < 0) {
      return;
    }

    // prepare event and refresh the calendar
    this.prepareCalendarEventBasedOnMeta(idx);
    this.refresh.next();

    // make sure that the user can update changes
    this.disableFootButtons = !this.isPeriodInBackend(event);
  }

  prepareCalendarEventBasedOnMeta(idx: number): void {
    const period = this.events[idx].meta;
    this.events[idx] = calendarPeriodForLockersToCalendarEvent(period);
  }

  addCalendarPeriodForLockersButtonClick(location: Location): void {
    this.addCalendarPeriodForLockers(location);
    this.disableFootButtons = false;
  }

  addCalendarPeriodForLockers(location: Location): void {
    const period: CalendarPeriodForLockers = CalendarPeriodForLockersConstructor.new();
    period.location = location;

    this.events = [
      ...this.events, calendarPeriodForLockersToCalendarEvent(period)
    ];
  }

  updateCalendarPeriodForLockersButtonClick(locationName: string): void {
    this.disableFootButtons = true;
    this.updateCalendarPeriodForLockers(locationName);
    this.setupEvents(locationName);
  }

  /**
   * This is the method that does all the CUD-work of
   * the CRUD operations available for CALENDAR_PERIODS_FOR_LOCKERS
   */
  updateCalendarPeriodForLockers(locationName: string): void {
    if (this.hasAnyPeriodChanged()) {
      // if this.events.length === 0, delete everything instead of updating
      if (this.events.length === 0) {
        this.deleteAllPeriodsInDataLayer(locationName);
        return;
      }

      // before updating or adding anything, check whether all periods are valid
      // Note: do not do this.events.forEach(handler), because the return in the 'handler'
      //   will return from the lambda, but not from the outer function and thus, a
      //   request will be sent to the backend, which is not wat we want if not all
      //   the periods are validly filled in
      for (const n of this.events) {
        if (!isCalendarPeriodForLockersValid(n.meta)) {
          this.handleWrongCalendarPeriodFormatOnUpdate();
          return;
        }
      }

      // if this.calendarPeriodsForLockersInDataLayer.length === 0, add all events instead of updating
      if (this.calendarPeriodsForLockersInDataLayer.length === 0) {
        this.addAllPeriodsInEvents(locationName);
        return;
      }

      // if reached here, persist update(s)
      this.calendarPeriodsForLockersService.updateCalendarPeriodsForLockers(
        this.calendarPeriodsForLockersInDataLayer,
        this.events.map<CalendarPeriodForLockers>(n => n.meta)
      ).subscribe(() => {
        this.successHandler(locationName);
      }, () => this.errorHandler());
    } else {
      this.handleNothingHasChangedOnUpdate();
    }
  }

  deleteAllPeriodsInDataLayer(locationName: string): void {
    this.calendarPeriodsForLockersService.deleteCalendarPeriodsForLockers(this.calendarPeriodsForLockersInDataLayer)
      .subscribe(() => this.successHandler(locationName), () => this.errorHandler());
  }

  addAllPeriodsInEvents(locationName: string): void {
    this.calendarPeriodsForLockersService
      .addCalendarPeriodsForLockers(this.events.map<CalendarPeriodForLockers>(n => n.meta))
      .subscribe(() => this.successHandler(locationName), () => this.errorHandler());
  }

  handleWrongCalendarPeriodFormatOnUpdate(): void {
    this.showWrongCalendarPeriodForLockersFormat = true;
    setTimeout(() => this.showWrongCalendarPeriodForLockersFormat = false, this.msToShowFeedback);
  }

  handleNothingHasChangedOnUpdate(): void {
    this.showSuccessButNoChanges = true;
    setTimeout(() => this.showSuccessButNoChanges = false, this.msToShowFeedback);
  }

  deleteCalendarPeriodForLockersButtonClick(event: CalendarEvent): void {
    this.deleteCalendarPeriodForLockers(event.meta);
    this.disableFootButtons = false;
  }

  deleteCalendarPeriodForLockers(period: CalendarPeriodForLockers): void {
    this.events = this.events.filter(n => n.meta !== period);
  }

  cancelChangesButtonClick(): void {
    this.events = this.mapCalendarPeriodsForLockersToCalendarEvents(this.calendarPeriodsForLockersInDataLayer);
    this.disableFootButtons = true;
  }

  successHandler(locationName: string): void {
    this.showSuccess = true;
    setTimeout(() => this.showSuccess = false, this.msToShowFeedback);
    // Refresh the events in the 'data layer' attribute
    this.setupEvents(locationName);
  }

  errorHandler(): void {
    this.showError = true;
    setTimeout(() => this.showError = false, this.msToShowFeedback);
  }
}
