// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false
};

export const api = {
  // CALENDAR_PERIODS
  calendarPeriods: '/api/locations/calendar/{locationName}',
  addCalendarPeriods: '/api/locations/calendar',
  updateCalendarPeriods: '/api/locations/calendar/{locationName}',
  deleteCalendarPeriods: '/api/locations/calendar',

  // CALENDAR_PERIODS_FOR_LOCKERS
  calendarPeriodsForLockers: '/api/locations/lockerCalendar/{locationName}',
  addCalendarPeriodsForLockers: '/api/locations/lockerCalendar',
  updateCalendarPeriodsForLockers: '/api/locations/lockerCalendar',
  deleteCalendarPeriodsForLockers: '/api/locations/lockerCalendar',

  // LOCATIONS
  locations: '/api/locations',
  location: '/api/locations/{locationName}',
  addLocation: '/api/locations',
  updateLocation: '/api/locations/{locationName}',
  deleteLocation: '/api/locations/{locationName}',
  numberOfReservations: '/api/locations/{locationName}/reservations/count',

  // LOCATION_RESERVATIONS
  locationReservationsByUserId: '/api/locations/reservations/{userId}',

  // LOCKERS
  lockersStatusesOfLocation: '/api/lockers/status/{locationName}',

  // LOCKER_RESERVATIONS
  lockerReservationsByUserId: '/api/lockers/reservations/{userId}',
  updateLockerReservation: '/api/lockers/reservations',

  // USERS
  userByAUGentId: '/api/account/id',
  userByBarcode: '/api/account/barcode',
  userByMail: '/api/account/mail',
  usersByFirstName: '/api/account/firstName',
  usersByLastName: '/api/account/lastName',
  usersByFirstAndLast: '/api/account/firstAndLastName',
  changePassword: '/api/account/password',
  updateUser: '/api/account/{id}',

  // PENALTY_BOOK
  penaltiesByUserId: '/api/penalties/{id}',
  addPenalty: '/api/penalties',
  deletePenalty: '/api/penalties'
};

export const vars = {
  defaultLocationImage: 'assets/images/default_location.jpg'
};

/*
 * The amount of milliseconds that a feedback div should be shown
 */
export const msToShowFeedback = 10000;

/*
 * The PenaltyEvent code for a manual entry
 */
export const penaltyEventCodeForManualEntry = 16663;

/*
 * The roles that are available in the application
 *
 * Important: make sure that the roles in 'rolesArray'
 * are put in the 'Role' enum as well!
 *
 * And, do not forget to translate the role if you would
 * be adding a role (<lang>.json -> general.roles)
 */
export const rolesArray = ['ADMIN', 'EMPLOYEE', 'STUDENT'];

export enum Role {
  STUDENT = 'STUDENT',
  EMPLOYEE = 'EMPLOYEE',
  ADMIN = 'ADMIN'
}

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/dist/zone-error';  // Included with Angular CLI.
