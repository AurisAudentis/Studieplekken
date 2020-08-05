/*
 * Setup a test user
 */
insert into public.users(augentid, role, augentpreferredgivenname, augentpreferredsn, penalty_points, mail, password, institution)
values ('001', 'STUDENT', 'Bram', 'Van de Walle', 0, 'bram.vandewalle@ugent.be', 'secret', 'UGent');

/*
 * Setup two test locations
 */
insert into public.locations (name, address, number_of_seats, number_of_lockers, image_url)
values ('Therminal', 'Hoveniersberg 24, 9000 Gent', 200, 100, 'www.example.png'),
('Sterre S5', 'Krijgslaan 281, 9000 Gent', 200, 100, 'www.example.png');

insert into public.location_descriptions (location_name, lang_enum, description)
values ('Therminal', 'ENGLISH', 'Go and study in the student house "De Therminal"'),
('Therminal', 'DUTCH', 'Studeer in het studentenhuis "De Therminal"'),
('Sterre S5', 'ENGLISH', 'Go and study in building S5 of the Sterre'),
('Sterre S5', 'DUTCH', 'Studeer in de S5 van de Sterre');

/*
 * Add some penalties for the test user
 */
insert into public.penalty_book(user_augentid, event_code, timestamp, reservation_date, received_points, reservation_location)
values ('001', 16660, replace(to_char(now() - interval '5 days', 'YYYY-MM-DD HH24:MI:SS'), ' ', 'T'),
        replace(to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), ' ', 'T'), 30, 'Therminal'),
       ('001', 16661, replace(to_char(now() - interval '4 days', 'YYYY-MM-DD HH24:MI:SS'), ' ', 'T'),
        replace(to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), ' ', 'T'), 30, 'Sterre S5');

/*
 * Setup all the lockers for the test locations
 */
insert into public.lockers (location_name, number)
values ('Therminal', 0),
('Therminal', 1),
('Therminal', 2),
('Therminal', 3),
('Therminal', 4),
('Therminal', 5),
('Therminal', 6),
('Therminal', 7),
('Therminal', 8),
('Therminal', 9),
('Therminal', 10),
('Therminal', 11),
('Therminal', 12),
('Therminal', 13),
('Therminal', 14),
('Therminal', 15),
('Therminal', 16),
('Therminal', 17),
('Therminal', 18),
('Therminal', 19),
('Therminal', 20),
('Therminal', 21),
('Therminal', 22),
('Therminal', 23),
('Therminal', 24),
('Therminal', 25),
('Therminal', 26),
('Therminal', 27),
('Therminal', 28),
('Therminal', 29),
('Therminal', 30),
('Therminal', 31),
('Therminal', 32),
('Therminal', 33),
('Therminal', 34),
('Therminal', 35),
('Therminal', 36),
('Therminal', 37),
('Therminal', 38),
('Therminal', 39),
('Therminal', 40),
('Therminal', 41),
('Therminal', 42),
('Therminal', 43),
('Therminal', 44),
('Therminal', 45),
('Therminal', 46),
('Therminal', 47),
('Therminal', 48),
('Therminal', 49),
('Therminal', 50),
('Therminal', 51),
('Therminal', 52),
('Therminal', 53),
('Therminal', 54),
('Therminal', 55),
('Therminal', 56),
('Therminal', 57),
('Therminal', 58),
('Therminal', 59),
('Therminal', 60),
('Therminal', 61),
('Therminal', 62),
('Therminal', 63),
('Therminal', 64),
('Therminal', 65),
('Therminal', 66),
('Therminal', 67),
('Therminal', 68),
('Therminal', 69),
('Therminal', 70),
('Therminal', 71),
('Therminal', 72),
('Therminal', 73),
('Therminal', 74),
('Therminal', 75),
('Therminal', 76),
('Therminal', 77),
('Therminal', 78),
('Therminal', 79),
('Therminal', 80),
('Therminal', 81),
('Therminal', 82),
('Therminal', 83),
('Therminal', 84),
('Therminal', 85),
('Therminal', 86),
('Therminal', 87),
('Therminal', 88),
('Therminal', 89),
('Therminal', 90),
('Therminal', 91),
('Therminal', 92),
('Therminal', 93),
('Therminal', 94),
('Therminal', 95),
('Therminal', 96),
('Therminal', 97),
('Therminal', 98),
('Therminal', 99),
('Sterre S5', 0),
('Sterre S5', 1),
('Sterre S5', 2),
('Sterre S5', 3),
('Sterre S5', 4),
('Sterre S5', 5),
('Sterre S5', 6),
('Sterre S5', 7),
('Sterre S5', 8),
('Sterre S5', 9),
('Sterre S5', 10),
('Sterre S5', 11),
('Sterre S5', 12),
('Sterre S5', 13),
('Sterre S5', 14),
('Sterre S5', 15),
('Sterre S5', 16),
('Sterre S5', 17),
('Sterre S5', 18),
('Sterre S5', 19),
('Sterre S5', 20),
('Sterre S5', 21),
('Sterre S5', 22),
('Sterre S5', 23),
('Sterre S5', 24),
('Sterre S5', 25),
('Sterre S5', 26),
('Sterre S5', 27),
('Sterre S5', 28),
('Sterre S5', 29),
('Sterre S5', 30),
('Sterre S5', 31),
('Sterre S5', 32),
('Sterre S5', 33),
('Sterre S5', 34),
('Sterre S5', 35),
('Sterre S5', 36),
('Sterre S5', 37),
('Sterre S5', 38),
('Sterre S5', 39),
('Sterre S5', 40),
('Sterre S5', 41),
('Sterre S5', 42),
('Sterre S5', 43),
('Sterre S5', 44),
('Sterre S5', 45),
('Sterre S5', 46),
('Sterre S5', 47),
('Sterre S5', 48),
('Sterre S5', 49),
('Sterre S5', 50),
('Sterre S5', 51),
('Sterre S5', 52),
('Sterre S5', 53),
('Sterre S5', 54),
('Sterre S5', 55),
('Sterre S5', 56),
('Sterre S5', 57),
('Sterre S5', 58),
('Sterre S5', 59),
('Sterre S5', 60),
('Sterre S5', 61),
('Sterre S5', 62),
('Sterre S5', 63),
('Sterre S5', 64),
('Sterre S5', 65),
('Sterre S5', 66),
('Sterre S5', 67),
('Sterre S5', 68),
('Sterre S5', 69),
('Sterre S5', 70),
('Sterre S5', 71),
('Sterre S5', 72),
('Sterre S5', 73),
('Sterre S5', 74),
('Sterre S5', 75),
('Sterre S5', 76),
('Sterre S5', 77),
('Sterre S5', 78),
('Sterre S5', 79),
('Sterre S5', 80),
('Sterre S5', 81),
('Sterre S5', 82),
('Sterre S5', 83),
('Sterre S5', 84),
('Sterre S5', 85),
('Sterre S5', 86),
('Sterre S5', 87),
('Sterre S5', 88),
('Sterre S5', 89),
('Sterre S5', 90),
('Sterre S5', 91),
('Sterre S5', 92),
('Sterre S5', 93),
('Sterre S5', 94),
('Sterre S5', 95),
('Sterre S5', 96),
('Sterre S5', 97),
('Sterre S5', 98),
('Sterre S5', 99);

