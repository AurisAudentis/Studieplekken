-- This file contains all the queries that are used within the application.
--
-- However, this file will not be read by Spring Boot to get the SQL queries.
-- The Java code uses the file database.properties as a key-value file to
-- get the queries.
--
-- This file is used as it is much easier to read/write/update formatted
-- queries. But, we need to convert this file into the key-value format.
-- Converting these queries can be done by:
--    - Windows: run `backend/scripts/update_database_properties.bat
--    - Unix: run `backend/scripts/update_database_properties.sh` with bash
--
-- Not only the queries in this file will be added to the database.properties
-- file, but the key-values within application_columns.props will be added
-- to the file as well.
--
-- Do note that it is important that this file is formatted correctly in
-- order for the formatting script to be able to correctly create the
-- database.properties file:
--     +-
--     | -- Start of a new block of queries, e.g. queries for a TABLE
--     | -- $name_of_query1.1
--     | ...
--     |  formatted query that must end with ';'
--     | ...;
--     |
--     | -- $name_of_query1.2 (after one newline)
--     | ...
--     |  formatted query that must end with ';'
--     | ...;
--     |
--     |
--     | -- Start of a new block of queries, after two \n
--     | -- $name_of_query2.1
--     | ...
--     |  formatted query that must end with ';'
--     | ...;
--     +-

-- queries for table LOCATIONS
-- $all_locations
select l.location_id, l.name, l.number_of_seats, l.number_of_lockers
    , l.image_url, l.description_dutch, l.description_english, l.for_group
    , b.building_id, b.building_name, b.address
    , a.authority_id, a.authority_name, a.description
from public.locations l
    join public.authority a
        on a.authority_id = l.authority_id
    join public.buildings b
        on b.building_id = l.building_id
where l.approved = true
order by l.name;

-- $all_locations_next_reservable_froms
select l.name, min(cp.reservable_from) as reservable_from
from public.locations l
    join public.calendar_periods cp
        on cp.location_id = l.location_id
where cp.reservable_from >= now()
group by l.name
order by l.name;

-- $get_location_by_name
select l.location_id, l.name, l.number_of_seats, l.number_of_lockers
    , l.image_url, l.description_dutch, l.description_english, l.for_group
    , b.building_id, b.building_name, b.address
    , a.authority_id, a.authority_name, a.description
from public.locations l
    join public.authority a
        on a.authority_id = l.authority_id
    join public.buildings b
        on b.building_id = l.building_id
where l.name = ?;

-- $get_location_by_id
select l.location_id, l.name, l.number_of_seats, l.number_of_lockers
     , l.image_url, l.description_dutch, l.description_english, l.for_group
     , b.building_id, b.building_name, b.address
     , a.authority_id, a.authority_name, a.description
from public.locations l
     join public.authority a
          on a.authority_id = l.authority_id
     join public.buildings b
          on b.building_id = l.building_id
where l.location_id = ?;

-- $get_locations_in_building
select l.location_id, l.name, l.number_of_seats, l.number_of_lockers
     , l.image_url, l.description_dutch, l.description_english, l.for_group
     , b.building_id, b.building_name, b.address
     , a.authority_id, a.authority_name, a.description
from public.locations l
         join public.authority a
              on a.authority_id = l.authority_id
         join public.buildings b
              on b.building_id = l.building_id
where l.building_id = ? and l.approved = true;

-- $delete_location
delete
from public.locations
where location_id = ?;

-- $insert_location
insert into public.locations (name, number_of_seats, number_of_lockers, image_url, authority_id, building_id, description_dutch, description_english, for_group, approved)
values (?, ?, ?, ?, ?, ?, ?, ?, ?, false) returning location_id;

-- $update_location
update public.locations
set name = ?, number_of_seats = ?, number_of_lockers = ?, image_url = ?, authority_id = ?, building_id = ?, description_dutch = ?, description_english = ?, for_group = ?
where location_id = ?;

-- $approve_location
update public.locations
set approved = ?
where location_id = ?;

-- $all_unapproved_locations
select l.location_id, l.name, l.number_of_seats, l.number_of_lockers
    , l.image_url, l.description_dutch, l.description_english, l.for_group
    , b.building_id, b.building_name, b.address
    , a.authority_id, a.authority_name, a.description
from public.locations l
    join public.authority a
        on a.authority_id = l.authority_id
    join public.buildings b
        on b.building_id = l.building_id
where l.approved = false
order by l.name;


-- $add_volunteer
insert into public.roles_user_volunteer (user_id, location_id)
values (?, ?) RETURNING location_id;

-- $delete_volunteer
delete from public.roles_user_volunteer v
where v.user_id = ? and v.location_id = ?;

-- $get_volunteers_of_location
select u.*
from public.roles_user_volunteer ruv
    INNER JOIN public.users u on u.user_id = ruv.user_id
where location_id = ?;

-- $get_locations_of_volunteer
select l.*, b.*, a.*
from public.roles_user_volunteer ruv
    join public.locations l
        on l.location_id = ruv.location_id
    join public.buildings b
        on b.building_id = l.building_id
    join public.authority a
        on a.authority_id = l.authority_id
where ruv.user_id = ?;


-- queries for table BUILDINGS
-- $all_buildings
select b.building_id, b.building_name, b.address
from public.buildings b
order by b.building_name;

-- $get_building_by_id
select b.building_id, b.building_name, b.address
from public.buildings b
where b.building_id = ?;

-- $add_building
insert into public.buildings (building_name, address)
values (?, ?) RETURNING building_id;

-- $update_building
update public.buildings
set building_name = ?, address = ?
where building_id = ?;

-- $delete_building
delete
from public.buildings b
where b.building_id = ?;


-- queries for table TAGS
-- $all_tags
select t.tag_id, t.dutch, t.english
from public.tags t
order by t.dutch;

-- $get_tag
select t.tag_id, t.dutch, t.english
from public.tags t
where t.tag_id = ?;

-- $add_tag
insert into public.tags (dutch, english)
values (?, ?) RETURNING tag_id;

-- $delete_tag
delete from tags
where tag_id = ?;

-- $update_tag
update public.tags
set dutch= ?, english = ?
where tag_id = ?;


-- queries for table LOCATION_RESERVATION
-- $get_location_reservations_where_<?>
select lr.*, u.*, rt.*
from public.location_reservations lr
    join public.users u
        on u.user_id = lr.user_id
    left join public.timeslots rt
        on rt.timeslot_date = lr.timeslot_date
        and rt.timeslot_sequence_number = lr.timeslot_seqnr
        and rt.calendar_id = lr.calendar_id
where <?>
order by u.first_name, u.last_name, u.user_id;

-- $count_location_reservations_of_location_for_timeslot
select count(1)
from public.location_reservations lr 
    INNER JOIN public.calendar_periods cp on lr.calendar_id = cp.calendar_id 
    INNER JOIN public.locations l on cp.location_id = l.location_id
where lr.calendar_id = ? and lr.timeslot_date = ? and lr.timeslot_seqnr = ?;

-- $add_one_to_reservation_count
update timeslots
set reservation_count = reservation_count + 1
where calendar_id = ? and timeslot_date = ? and timeslot_sequence_number= ?;

-- $subtract_one_to_reservation_count
update timeslots
set reservation_count = reservation_count - 1
where calendar_id = ? and timeslot_date = ? and timeslot_sequence_number= ?;

-- $subtract_x_to_reservation_count
update timeslots
set reservation_count = reservation_count - ?
where calendar_id = ? and timeslot_date = ? and timeslot_sequence_number= ?;


-- $delete_location_reservation
delete
from public.location_reservations
where user_id = ? and timeslot_date = ? and timeslot_seqnr = ? and calendar_id = ?;

-- $insert_location_reservation
insert into public.location_reservations (user_id, created_at, timeslot_date, timeslot_seqnr, calendar_id, attended)
values (?, ?, ?, ?, ?, null);

-- $set_location_reservation_attendance
update public.location_reservations
set attended = ?
where calendar_id = ? and timeslot_date = ? and timeslot_seqnr = ? and user_id = ?;

-- $get_location_reservations_with_location_by_user
select lr.*, cp.*, l.*, b.*, a.*, u.*, rt.reservation_count, rt.seat_count
     , lr.timeslot_seqnr as "timeslot_sequence_number"
from public.location_reservations lr
    join public.calendar_periods cp
        on cp.calendar_id = lr.calendar_id
    join public.locations l
        on l.location_id = cp.location_id
    join public.buildings b
        on b.building_id = l.building_id
    join public.authority a
        on a.authority_id = l.authority_id
    join users u
        on u.user_id = lr.user_id
    join public.timeslots rt
        on rt.timeslot_date = lr.timeslot_date and rt.timeslot_sequence_number = lr.timeslot_seqnr and rt.calendar_id = lr.calendar_id
where lr.user_id = ?
order by lr.timeslot_date desc, lr.timeslot_seqnr desc;

-- $get_unattended_reservations_on_date
with x as (
    select lr.*, rt.*, u.*, cp.*, l.*, b.*, a.*, row_number() over (partition by u.user_id) as rn
    from public.location_reservations lr
        join public.users u
            on u.user_id = lr.user_id
        join public.timeslots rt
             on rt.timeslot_date = lr.timeslot_date
             and rt.timeslot_sequence_number = lr.timeslot_seqnr
             and rt.calendar_id = lr.calendar_id
        join public.calendar_periods cp
            on cp.calendar_id = lr.calendar_id
        join public.locations l
             on l.location_id = cp.location_id
        join public.buildings b
             on b.building_id = l.building_id
        join public.authority a
             on a.authority_id = l.authority_id
    where lr.attended = false and lr.timeslot_date = ?
)
select *
from x where
rn = 1;

-- $get_users_with_reservation_in_window_of_time
with x as (
    select u.*, row_number() over (partition by u.user_id) as rn
    from public.location_reservations lr
        join public.users u
            on u.user_id = lr.user_id
    where lr.timeslot_date >= ? and lr.timeslot_date < ?
)
select *
from x where
rn = 1;

-- $set_not_scanned_students_as_not_attended
update public.location_reservations lr
set attended = False
where lr.calendar_id = ? and lr.timeslot_seqnr = ? and lr.timeslot_date = ? and lr.attended is null;


-- queries for table USER
-- $get_user_by_<?>
select u.*, 0 as "penalty_points"
from public.users u
where <?>
group by u.user_id, u.admin, u.first_name, u.last_name, u.mail, u.password, u.institution
order by u.last_name, u.first_name, u.user_id;

-- $update_user
update public.users
set mail = ?, last_name = ?, first_name = ?, password = ?, institution = ?, user_id = ?, admin = ?, penalty_points = ?
where user_id = ?;

-- $get_admins
select * from public.users where admin = true;

-- $count_accounts_with_email
select count(1)
from public.users
where LOWER(mail) = LOWER(?);

-- $insert_user
insert into public.users (mail, last_name, first_name, password, institution, user_id, admin, penalty_points)
values (?, ?, ?, ?, ?, ?, ?, ?);

-- $delete_user
delete
from public.users
where user_id = ?;


-- queries for table ROLES_USER_AUTHORITY
-- $insert_role_user_authority
insert into public.roles_user_authority (user_id, authority_id)
values (?, ?);

-- $remove_role_user_authority
delete
from public.roles_user_authority
where user_id = ? and authority_id = ?;


-- queries for table AUTHORITY
-- $all_authorities
select a.authority_id, a.authority_name, a.description
from public.authority a
order by a.authority_name;

-- $authorities_from_user
select a.authority_id, a.authority_name, a.description
from public.authority a
  join public.roles_user_authority roles on a.authority_id = roles.authority_id
  join public.users u on roles.user_id = u.user_id
  where u.user_id = ?
order by a.authority_name;

-- $authority_get_users
select u.user_id, u.admin, u.first_name, u.last_name, u.penalty_points, u.mail, u.institution
from public.users u
         join public.roles_user_authority roles on u.user_id = roles.user_id
         join public.authority a on roles.authority_id = a.authority_id
where a.authority_id = ?
order by u.user_id;

-- $authority_from_name
select a.authority_id, a.authority_name, a.description
from public.authority a
where a.authority_name = ?;

-- $authority_from_authority_id
select a.authority_id, a.authority_name, a.description
from public.authority a
where a.authority_id = ?;

-- $insert_authority
insert into public.authority (authority_name, description)
values (?, ?) RETURNING authority_id;

-- $update_authority
update public.authority
set authority_name = ?, description = ?
where authority_id = ?;

-- $delete_authority
delete
from public.authority
where authority_id = ?;

-- $get_locations_manageable_by_user
select l.*, a.*, b.*
from public.locations l
    join authority a
        on l.authority_id = a.authority_id
    join buildings b
        on b.building_id = l.building_id
    join roles_user_authority rua
        on rua.authority_id = a.authority_id
where rua.user_id = ?
order by l.name;

-- $get_locations_in_authority
select l.*, a.*, b.*
from public.locations l
         join authority a
              on l.authority_id = a.authority_id
         join buildings b
              on b.building_id = l.building_id
where a.authority_id = ?
order by l.name;


-- queries for DBPenaltyEventsDao
-- $get_penalty_events
select e.code, e.points, d.lang_enum, d.description
from penalty_events e
    join penalty_descriptions d
        on e.code = d.event_code
order by e.code;

-- $get_penalty_event
select e.code, e.points, d.lang_enum, d.description
from penalty_events e
    join penalty_descriptions d
        on e.code = d.event_code
where e.code = ?;

-- $get_penalties_by_user
select b.user_id, b.event_code, b.timestamp, b.reservation_date
    , b.received_points, b.reservation_location_id, b.remarks
from public.penalty_book b
where b.user_id = ?;

-- $get_penalties_by_location
select b.user_id, b.event_code, b.timestamp, b.reservation_date
     , b.received_points, b.reservation_location_id, b.remarks
from public.penalty_book b
where b.reservation_location_id = ?;

-- $get_penalties_by_event_code
select b.user_id, b.event_code, b.timestamp, b.reservation_date
     , b.received_points, b.reservation_location_id, b.remarks
from public.penalty_book b
where b.event_code = ?;

-- $insert_penalty_event
insert into public.penalty_events (code, points)
values (?, ?);

-- $insert_penalty
insert into penalty_book (user_id, event_code, timestamp, reservation_date, reservation_location_id, received_points, remarks)
values (?, ?, ?, ?, ?, ?, ?);

-- $insert_penalty_description
insert into public.penalty_descriptions (lang_enum, event_code, description)
values (?, ?, ?);

-- $update_penalty_event
update public.penalty_events
set code = ?, points = ?
where code = ?;

-- $delete_penalty_description
delete
from public.penalty_descriptions
where lang_enum = ? and event_code = ?;

-- $delete_penalty_descriptions_by_event_code
delete
from public.penalty_descriptions
where event_code = ?;

-- $delete_penalty_event
delete
from public.penalty_events
where code = ?;

-- $delete_penalty
delete
from public.penalty_book b
where b.user_id = ? and b.event_code = ? and b.timestamp = ?;

-- $count_descriptions_of_penalty_events
select count(1)
from public.penalty_descriptions
where event_code = ?;


-- queries for SCANNERS_LOCATION
-- $get_locations_of_scanner
select l.location_id, l.name, l.number_of_seats, l.number_of_lockers, l.image_url, l.description_dutch, l.description_english, l.for_group
       , a.authority_id, a.authority_name, a.description
       , b.building_id, b.building_name, b.address
from public.scanners_location sl
    join public.locations l
        on l.location_id = sl.location_id
    join public.authority a
        on a.authority_id = l.authority_id
    join public.buildings b
        on b.building_id = l.building_id
where sl.user_id = ?
order by l.name;

-- $get_scanners_of_location
select u.*
from public.scanners_location sl
    join public.users u
        on u.user_id = sl.user_id
where sl.location_id = ?;

-- $delete_scanner_location
delete
from public.scanners_location
where location_id = ? and user_id = ?;

-- $delete_scanners_of_location
delete from public.scanners_location
where location_id = ?;

-- $delete_locations_of_scanner
delete from public.scanners_location
where user_id = ?;

-- $insert_scanner_on_location
insert into public.scanners_location (location_id, user_id)
values (?, ?);

-- $count_scanner_on_location
select count(1)
from public.scanners_location
where user_id = ? and location_id = ?;


-- queries for LOCATION_TAG
-- $get_tags_for_location
select t.tag_id, t.dutch, t.english
from public.location_tags lt
    join public.tags t
        on t.tag_id = lt.tag_id
where lt.location_id = ?;

-- $get_locations_for_tag
select l.location_id, l.name, l.number_of_seats, l.number_of_lockers
        , l.image_url, l.description_dutch, l.description_english, l.for_group
        , a.authority_id, a.authority_name, a.description
        , b.building_id, b.building_name, b.address
from public.locations l
    join public.location_tags lt
        on l.location_id = lt.location_id
    join public.authority a
        on a.authority_id = l.authority_id
    join public.buildings b
        on b.building_id = l.building_id
where lt.tag_id = ?;

-- $add_tag_to_location
insert into public.location_tags (location_id, tag_id)
values (?, ?)
on conflict(location_id, tag_id) do nothing;

-- $delete_tag_from_location
delete
from public.location_tags
where location_id = ? and tag_id = ?;

-- $delete_all_tags_from_location
delete
from public.location_tags
where location_id = ?;

-- $delete_tag_from_all_locations
delete
from public.location_tags
where tag_id = ?;


-- queries for CALENDAR_PERIODS
-- $get_all_calendar_periods
select cp.calendar_id, cp.location_id, cp.starts_at, cp.ends_at, cp.opening_time, cp.closing_time, cp.reservable_from, cp.reservable, cp.timeslot_length, cp.locked_from
       , l.location_id, l.name, l.number_of_seats, l.number_of_lockers, l.image_url, l.description_dutch, l.description_english, l.for_group, b.building_id, b.building_name, b.address
       , a.authority_id, a.authority_name, a.description
from public.calendar_periods cp
    join public.locations l
        on l.location_id = cp.location_id
    join public.authority a
        on a.authority_id = l.authority_id
    join public.buildings b
        on l.building_id = b.building_id
order by to_date(cp.starts_at || ' ' || cp.opening_time, 'YYYY-MM-DD HH24:MI');

-- $get_calendar_periods
select cp.calendar_id, cp.location_id, cp.starts_at, cp.ends_at, cp.opening_time, cp.closing_time, cp.reservable_from, cp.reservable, cp.timeslot_length, cp.locked_from, cp.seat_count
       , l.location_id, l.name, l.number_of_seats, l.number_of_lockers, l.image_url, l.description_dutch, l.description_english, l.for_group
       , a.authority_id, a.authority_name, a.description
       , b.building_id, b.building_name, b.address
from public.calendar_periods cp
    join public.locations l
        on l.location_id = cp.location_id
    join public.authority a
        on a.authority_id = l.authority_id
    join public.buildings b
        on b.building_id = l.building_id
where cp.location_id = ?
order by cp.starts_at, cp.opening_time;

-- $insert_calendar_period
insert into public.calendar_periods(location_id, starts_at, ends_at, opening_time, closing_time, reservable_from, reservable, timeslot_length, locked_from, seat_count)
values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- $update_calendar_period
update public.calendar_periods
set location_id = ?, starts_at = ?, ends_at = ?, opening_time = ?, closing_time = ?, reservable_from = ?, reservable = ?, timeslot_length = ?, locked_from = ?
where calendar_id = ?;

-- $get_calendar_period_by_id
select * 
from public.calendar_periods cp inner join public.locations l on cp.location_id = l.location_id inner join public.buildings b on b.building_id = l.building_id
inner join public.authority a on a.authority_id = l.authority_id
where cp.calendar_id = ?;

-- $delete_calendar_period
delete
from public.calendar_periods
where location_id = ? and starts_at = ? and ends_at = ? and opening_time = ? and closing_time = ? and reservable = ? and timeslot_length = ?;

-- $get_calendar_periods_in_period
select * 
from public.calendar_periods cp
join public.locations l
    on l.location_id = cp.location_id
join public.authority a
    on a.authority_id = l.authority_id
join public.buildings b
    on b.building_id = l.building_id
where cp.starts_at > ? and cp.starts_at < ?;

-- $get_timeslots
select rt.timeslot_sequence_number, rt.timeslot_date, rt.calendar_id, rt.reservation_count, rt.seat_count
from public.timeslots rt
where calendar_id = ? 
order by rt.timeslot_date, rt.timeslot_sequence_number;

-- $insert_timeslots
insert into public.timeslots(calendar_id, timeslot_sequence_number, timeslot_date, seat_count)
values (?, ?, ?, ?);

-- $count_reservations_now
with y as (
select ts.calendar_id as calendar_id_dist, *
from public.locations l 
    join public.calendar_periods cp
        on l.location_id = cp.location_id
    join public.timeslots ts
        on ts.calendar_id = cp.calendar_id
    where cp.location_id = ?
    and ts.timeslot_date = current_date 
    and  cp.opening_time + (cp.timeslot_length * ts.timeslot_sequence_number) * INTERVAL '1 minute' <= current_time 
    and cp.opening_time + (cp.timeslot_length * (ts.timeslot_sequence_number + 1)) * INTERVAL '1 minute' >= current_time
)
select count(1) as reservation_count, (select count(1) from y) as timeslot_count
from y
    inner join public.location_reservations rs
        on y.timeslot_date = rs.timeslot_date
        and rs.timeslot_seqnr = y.timeslot_sequence_number
        and calendar_id_dist = rs.calendar_id;

-- $delete_timeslots_of_calendar
delete
from public.timeslots rt
where rt.calendar_id = ?;

-- $get_current_and_or_next_timeslot
with x as (
    select rt.calendar_id, rt.timeslot_sequence_number, rt.timeslot_date, rt.reservation_count, rt.seat_count
         , cp.location_id, cp.starts_at, cp.ends_at, cp.opening_time, cp.closing_time, cp.reservable_from, cp.locked_from, cp.reservable, cp.timeslot_length, cp.seat_count
         , (rt.timeslot_date + cp.opening_time)::timestamp + interval '1 minute' * cp.timeslot_length * rt.timeslot_sequence_number as timeslot_start
         , (rt.timeslot_date + cp.opening_time)::timestamp + interval '1 minute' * cp.timeslot_length * (rt.timeslot_sequence_number + 1) as timeslot_end
    from timeslots rt
             join calendar_periods cp
                  on cp.calendar_id = rt.calendar_id
    where cp.location_id = ?
      and (rt.timeslot_date + cp.opening_time)::timestamp + interval '1 minute' * cp.timeslot_length * (rt.timeslot_sequence_number + 1) > now()
), y as (
    select x.*, row_number() over(order by timeslot_start) n
    from x
)
select *
from y
where n = 1
order by timeslot_start;


-- miscellaneous queries
-- $get_week_overview_with_monday_and_sunday_dates
with x as (
    select t.timeslot_date, cp.opening_time, cp.closing_time, l.name as location_name
    from public.timeslots t
             join public.calendar_periods cp
                  on cp.calendar_id = t.calendar_id
             join public.locations l
                  on l.location_id = cp.location_id
    where t.timeslot_date >= ? and t.timeslot_date <= ?
), y as (
    select timeslot_date, location_name
         , min(opening_time) as opening_time
         , max(closing_time) as closing_time
    from x
    group by timeslot_date, location_name
), z as (
    select timeslot_date, location_name
         , case when extract(dow from timeslot_date) = 1 then to_char(opening_time, 'HH24:MI') || ' - ' || to_char(closing_time, 'HH24:MI') end as monday
         , case when extract(dow from timeslot_date) = 2 then to_char(opening_time, 'HH24:MI') || ' - ' || to_char(closing_time, 'HH24:MI') end as tuesday
         , case when extract(dow from timeslot_date) = 3 then to_char(opening_time, 'HH24:MI') || ' - ' || to_char(closing_time, 'HH24:MI') end as wednesday
         , case when extract(dow from timeslot_date) = 4 then to_char(opening_time, 'HH24:MI') || ' - ' || to_char(closing_time, 'HH24:MI') end as thursday
         , case when extract(dow from timeslot_date) = 5 then to_char(opening_time, 'HH24:MI') || ' - ' || to_char(closing_time, 'HH24:MI') end as friday
         , case when extract(dow from timeslot_date) = 6 then to_char(opening_time, 'HH24:MI') || ' - ' || to_char(closing_time, 'HH24:MI') end as saturday
         , case when extract(dow from timeslot_date) = 0 then to_char(opening_time, 'HH24:MI') || ' - ' || to_char(closing_time, 'HH24:MI') end as sunday
    from y
), p as (
    select location_name
         , max(monday) as monday
         , max(tuesday) as tuesday
         , max(wednesday) as wednesday
         , max(thursday) as thursday
         , max(friday) as friday
         , max(saturday) as saturday
         , max(sunday) as sunday
    from z
    group by location_name
)
select *
from p
order by location_name;
