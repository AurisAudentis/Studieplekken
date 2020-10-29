// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export enum APPLICATION_TYPE {
  BLOK_AT,
  MINI_THERMIS
}

export const environment = {
  production: false,
  applicationType: APPLICATION_TYPE.MINI_THERMIS
};

export const api = {
  whoAmI: '/whoAmI',
  logout: '/logout',

  // AUTHORITY
  authorities: '/api/authority',
  authority: '/api/authority/{authorityId}',
  addAuthority: '/api/authority',
  updateAuthority: '/api/authority/{authorityId}',
  deleteAuthority: '/api/authority/{authorityId}',
  locationsInAuthoritiesOfUser: '/api/authority/users/{userId}/locations',

  // CALENDAR_PERIODS
  calendarPeriods: '/api/locations/calendar/{locationName}',
  addCalendarPeriods: '/api/locations/calendar',
  updateCalendarPeriods: '/api/locations/calendar/{locationName}',
  deleteCalendarPeriods: '/api/locations/calendar',

  // CALENDAR_PERIODS_FOR_LOCKERS
  calendarPeriodsForLockers: '/api/locations/lockerCalendar/{locationName}',
  addCalendarPeriodsForLockers: '/api/locations/lockerCalendar',
  updateCalendarPeriodsForLockers: '/api/locations/lockerCalendar/{locationName}',
  deleteCalendarPeriodsForLockers: '/api/locations/lockerCalendar',

  // LOCATIONS
  locations: '/api/locations',
  location: '/api/locations/{locationName}',
  addLocation: '/api/locations',
  updateLocation: '/api/locations/{locationName}',
  deleteLocation: '/api/locations/{locationName}',
  setupTagsForLocation: '/api/locations/tags/{locationName}',

  // LOCATION_RESERVATIONS
  locationReservationsOfUser: '/api/locations/reservations/user',
  locationReservationsOfLocation: '/api/locations/reservations/timeslot/{calendarid}/{date}/{seqnr}',
  locationReservationsOfLocationFrom: '/api/locations/reservations/from',
  locationReservationsOfLocationUntil: '/api/locations/reservations/until',
  locationReservationsOfLocationFromAndUntil: '/api/locations/reservations/fromAndUntil',
  addLocationReservation: '/api/locations/reservations/new',
  deleteLocationReservation: '/api/locations/reservations',

  // LOCKER_RESERVATIONS
  lockerReservationsOfUser: '/api/lockers/reservations/user',
  lockerReservationsOfLocation: '/api/lockers/reservations/location',
  updateLockerReservation: '/api/lockers/reservations',
  deleteLockerReservation: '/api/lockers/reservations',

  // USERS
  userByAUGentId: '/api/account/id',
  userByBarcode: '/api/account/barcode',
  usersByFirstName: '/api/account/firstName',
  usersByLastName: '/api/account/lastName',
  usersByFirstAndLast: '/api/account/firstAndLastName',
  changePassword: '/api/account/password',
  updateUser: '/api/account/{userId}',
  hasUserAuthorities: '/api/account/{userId}/has/authorities',

  // LOCKERS
  lockersStatusesOfLocation: '/api/lockers/status/{locationName}',

  // PENALTY_BOOK
  penaltiesByUserId: '/api/penalties/{id}',
  addPenalty: '/api/penalties',
  deletePenalty: '/api/penalties',

  // PENALTY_EVENTS
  penaltyEvents: '/api/penalties/events',
  addPenaltyEvent: '/api/penalties/events',
  updatePenaltyEvent: '/api/penalties/events/{code}',
  deletePenaltyEvent: '/api/penalties/events',

  // TAGS
  tags: '/api/tags',
  addTag: '/api/tags',
  updateTag: '/api/tags',
  deleteTag: '/api/tags/{tagId}',

  // ROLES_USER_AUTHORITY
  usersInAuthority: '/api/authority/{authorityId}/users',
  authoritiesOfUser: '/api/authority/users/{userId}',
  addUserToAuthority: '/api/authority/{authorityId}/user/{userId}',
  deleteUserFromAuthority: '/api/authority/{authorityId}/user/{userId}'
};

export const vars = {
  defaultLocationImage: 'assets/images/default_location.jpg',
  casFlowTriggerUrl: 'https://localhost:8080/login/cas',
  userWantsTLogInLocalStorageKey: 'userWantsTLogIn'
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
 * The roles that are available in the application.
 * This is used for being able to manage the role(s)
 * of a certain user.
 *
 * Do not forget to translate the role if you would
 * be adding a role (<lang>.json -> general.roles)
 */
export const rolesArray = ['ADMIN'];

/*
 * This variable maps all the supported languages to its
 * database representation (LANGUAGES.enum)
 */
export const languageAsEnum = {
  nl: 'DUTCH',
  en: 'ENGLISH'
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/dist/zone-error';  // Included with Angular CLI.
