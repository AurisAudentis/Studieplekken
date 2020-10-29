
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
DECLARE tableId integer;
BEGIN
  INSERT INTO authority (authority_name, description)
    VALUES ('DSA', 'Dienst StudentenActiviteiten') RETURNING authority_id into tableId;
  INSERT INTO locations (name, address, number_of_seats, number_of_lockers, image_url, authority_id, description_dutch, description_english)
    VALUES  ('Turbinezaal', 'Hoveniersberg 24, 9000 Gent', 50, 100, '', tableId, 'neder', 'engl'),
            ('Plenaire vergaderzaal', 'Hoveniersberg 24, 9000 Gent', 30, 0, '', tableId, '', ''),
            ('Podiumzaal', 'Hoveniersberg 24, 9000 Gent', 100, 0, '', tableId, '', ''),
            ('Trechterzaal', 'Hoveniersberg 24, 9000 Gent', 80, 0, '', tableId, '', '');
  INSERT INTO roles_user_authority (user_id, authority_id) VALUES ('002',tableId);
END $$;


DO $$
DECLARE new_authority_id integer;
BEGIN
  INSERT INTO authority (authority_name, description) VALUES ('WE', 'Faculteit wetenschappen') RETURNING authority_id into new_authority_id;
  INSERT INTO locations (name, address, number_of_seats, number_of_lockers, image_url, authority_id, description_dutch, description_english)
    VALUES  ('Sterre S9, PC lokaal 3rd verdiep', 'Krijgslaan 281, 9000 Gent', 40, 100, '', new_authority_id, 'Klaslokaal met computers', 'Classroom with computers'),
            ('Sterre S5, Bib', 'Krijgslaan 281, 9000 Gent', 100, 100, '', new_authority_id,
                    'Informatie over de bib kan hier gevonden worden: https://lib.ugent.be/nl/libraries/WEBIB.',
                    'Information about the bib itself can be found here: https://lib.ugent.be/nl/libraries/WEBIB.'),
            ('Sterre S5, Eetzaal', 'Krijgslaan 281, 9000 Gent', 130, 100, '', new_authority_id, '', '');
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
insert into calendar_periods(location_name, starts_at, ends_at, opening_time, closing_time, reservable_from, reservable, timeslot_length)
values  ('Sterre S5, Eetzaal', to_char(now() - interval '1 days', 'YYYY-MM-DD'), to_char(now() + interval '3 days', 'YYYY-MM-DD'),
            '10:00', '12:00', to_char(now() - interval '7 days', 'YYYY-MM-DD') || ' 19:00', true, 60),
        ('Sterre S5, Bib', to_char(now() - interval '5 days', 'YYYY-MM-DD'), to_char(now() + interval '10 days', 'YYYY-MM-DD'),
            '09:00', '17:00', to_char(now() - interval '7 days', 'YYYY-MM-DD') || ' 19:00', false, 0),
        ('Sterre S9, PC lokaal 3rd verdiep', to_char(now() - interval '5 days', 'YYYY-MM-DD'), to_char(now() + interval '10 days', 'YYYY-MM-DD'),
            '8:30', '18:30', to_char(now() - interval '7 days', 'YYYY-MM-DD') || ' 19:00', false, 0);


insert into reservation_timeslots(calendar_id, timeslot_sequence_number, timeslot_date)
values 
(1, 0,  to_char(now() - interval '1 days', 'YYYY-MM-DD')),
(1, 1,  to_char(now() - interval '1 days', 'YYYY-MM-DD')),
(1, 0,  to_char(now(), 'YYYY-MM-DD')),
(1, 1,  to_char(now(), 'YYYY-MM-DD')),
(1, 0,  to_char(now()+ interval '1 days', 'YYYY-MM-DD')),
(1, 1,  to_char(now()+ interval '1 days', 'YYYY-MM-DD')),
(1, 0,  to_char(now()+ interval '2 days', 'YYYY-MM-DD')),
(1, 1,  to_char(now()+ interval '2 days', 'YYYY-MM-DD')),
(1, 0,  to_char(now()+ interval '3 days', 'YYYY-MM-DD')),
(1, 1,  to_char(now()+ interval '3 days', 'YYYY-MM-DD'));


/*
 * Add some penalties for the test user
 */
insert into location_reservations(created_at, timeslot_date, timeslot_seqnr, calendar_id, user_augentid)
values
-- One reservation for over five days
(to_char(now() + interval '5 days', 'YYYY-MM-DD'),  to_char(now() + interval '1 days', 'YYYY-MM-DD'), 0, 1, '001'),
-- One reservation for five days ago, attended to
(to_char(now() + interval '5 days', 'YYYY-MM-DD'),  to_char(now() + interval '3 days', 'YYYY-MM-DD'), 0, 1, '001');
