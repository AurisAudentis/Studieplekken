
-- Initial seeding of a database with data

/*
 * Setup users
 */
INSERT into users(augentid, augentpreferredgivenname, augentpreferredsn, penalty_points, mail, password, institution, admin)
VALUES ('001', 'Bram', 'Van de Walle', 0, 'bram.vandewalle@ugent.be', 'secret', 'UGent', true);

INSERT into users(augentid, augentpreferredgivenname, augentpreferredsn, penalty_points, mail, password, institution, admin)
VALUES ('002', 'Ruben_van_DSA', 'DF', 0, 'rdf@ugent.be', 'secret', 'UGent', true);

INSERT into users(augentid, augentpreferredgivenname, augentpreferredsn, penalty_points, mail, password, institution, admin)
VALUES ('003', 'Maxime', 'Bloch', 0, 'maxime.bloch@ugent.be', 'secret', 'UGent', true);


/*
 * Setup third test user
 */
insert into public.users(augentid, augentpreferredgivenname, augentpreferredsn, penalty_points, mail, password, institution, admin)
values ('01405190', 'Tim', 'Van Erum', 0, 'tim.vanerum@ugent.be', 'secret', 'UGent', true);

/*
 * Setup two test locations with an authority and add 2nd test user to the authority
 */
DO $$
DECLARE auth_id integer;
DECLARE build_id_therminal integer;

BEGIN
  INSERT INTO public.authority (authority_name, description) values ('DSA', 'Dienst StudentenActiviteiten') RETURNING authority_id into auth_id;
  INSERT INTO public.buildings (building_name, address) VALUES ('Therminal', 'Hoveniersberg 24, 9000 Gent') RETURNING building_id into build_id_therminal;
  INSERT INTO public.locations (name, building_id, number_of_seats, number_of_lockers, image_url, authority_id, description_dutch, description_english, forGroup)
    VALUES  ('Turbinezaal', build_id_therminal, 50, 100, '', auth_id, 'neder', 'engl', false),
                ('Plenaire vergaderzaal', build_id_therminal, 30, 0, '', auth_id, '', '', true),
                ('Podiumzaal', build_id_therminal, 100, 0, '', auth_id, '', '', false),
                ('Trechterzaal', build_id_therminal, 80, 0, '', auth_id, '', '', false);
  INSERT INTO public.roles_user_authority (user_id, authority_id) VALUES ('002', auth_id);
END $$;


DO $$
DECLARE new_authority_id integer;
DECLARE build_id_sterreS5 integer;
DECLARE build_id_sterreS9 integer;

BEGIN
  INSERT INTO authority (authority_name, description) VALUES ('WE', 'Faculteit wetenschappen') RETURNING authority_id into new_authority_id;
  INSERT INTO public.buildings (building_name, address) VALUES ('Sterre S5', 'Krijgslaan 281, 9000 Gent') RETURNING building_id into build_id_sterreS5;
  INSERT INTO public.buildings (building_name, address) VALUES ('Sterre S9', 'Krijgslaan 281, 9000 Gent') RETURNING building_id into build_id_sterreS9;
  INSERT INTO locations (name, building_id, number_of_seats, number_of_lockers, image_url, authority_id, description_dutch, description_english, forGroup)
    VALUES  ('Sterre S9, PC lokaal 3rd verdiep', build_id_sterreS9, 40, 100, '', new_authority_id, 'Klaslokaal met computers', 'Classroom with computers', false),
            ('Sterre S5, Bib', build_id_sterreS5, 100, 100, '', new_authority_id,
                    'Informatie over de bib kan hier gevonden worden: https://lib.ugent.be/nl/libraries/WEBIB.',
                    'Information about the bib itself can be found here: https://lib.ugent.be/nl/libraries/WEBIB.', false),
            ('Sterre S5, Eetzaal', build_id_sterreS5, 130, 100, '', new_authority_id, '', '', false);
  INSERT INTO roles_user_authority (user_id, authority_id) VALUES ('002', new_authority_id);
END $$;
/*
 * add tags and use them in location Therminal
 */
DO $$
DECLARE new_tag_id integer;
BEGIN
    INSERT INTO tags (dutch, english) values ('eetplaats', 'dinner place') RETURNING tag_id into new_tag_id;
    INSERT INTO location_tags (location_id, tag_id) values ('Sterre S5, Eetzaal', new_tag_id);

    INSERT INTO tags (dutch, english) values ('stilte', 'silencium') RETURNING tag_id into new_tag_id;
    INSERT INTO location_tags (location_id, tag_id) values ('Sterre S5, Bib', new_tag_id);

    INSERT INTO tags (dutch, english) values ('computers', 'computers') RETURNING tag_id into new_tag_id;
    INSERT INTO location_tags (location_id, tag_id) values ('Sterre S9, PC lokaal 3rd verdiep', new_tag_id);
END $$;
/*
 * Add some calendar periods
 */
insert into calendar_periods(location_name, starts_at, ends_at, opening_time, closing_time, reservable_from, reservable, timeslot_length, locked_from)
values  ('Sterre S5, Eetzaal', now() - interval '1 days', now() + interval '3 days',
            '10:00', '12:00', now() - interval '7 days', true, 60, now() + interval '1 week'),
        ('Sterre S5, Bib', now() - interval '5 days', now() + interval '10 days',
            '09:00', '17:00', now() - interval '7 days', false, 0, now() + interval '1 week'),
        ('Sterre S9, PC lokaal 3rd verdiep',now() - interval '5 days', now() + interval '10 days',
            '8:30', '18:30', now() - interval '7 days', false, 0, now() + interval '1 week');


insert into reservation_timeslots(calendar_id, timeslot_sequence_number, timeslot_date)
values 
(1, 0,  now() - interval '1 days'),
(1, 1,  now() - interval '1 days'),
(1, 0,  now()),
(1, 1,  now()),
(1, 0,  now()+ interval '1 days'),
(1, 1,  now()+ interval '1 days'),
(1, 0,  now()+ interval '2 days'),
(1, 1,  now()+ interval '2 days'),
(1, 0,  now()+ interval '3 days'),
(1, 1,  now()+ interval '3 days');


/*
 * Add some penalties for the test user
 */
insert into location_reservations(created_at, timeslot_date, timeslot_seqnr, calendar_id, user_augentid)
values
-- One reservation for over five days
(now() + interval '5 days',  now() + interval '1 days', 0, 1, '001'),
-- One reservation for five days ago, attended to
(now() + interval '5 days',  now() + interval '3 days', 0, 1, '001');
