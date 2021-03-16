# BlokAtUGent

[![Build Status](https://ci.dsa.ugent.be/api/badges/DSA/BlokAt/status.svg)](https://ci.dsa.ugent.be/DSA/BlokAt)


Have a look at our [Wiki](https://github.ugent.be/bravdwal/dsa/wiki)

## Development

### Backend
**Requirements**
- Java 8
- PostgreSQL 12

**Configure**  
***PostgreSQL***

Make sure that a user `postgres` with password `postgres` has following databases, running on (<b>default</b>) port 5432:
- `blokatugent`
- `blokatugent_test`

For the `blokatugent` database, run the script `scripts/database/seed.sql` to provide some dummy data.

Configure your correct database configuration in de `application.yml`properties file in the resources folder.

***Self-signed certificate for HTTPS***  

To be able to use CAS in development, the callback URL that the UGent CAS Server uses, <i>has</i> to be `https://localhost:8080/**`.

Therefore, we need to set up HTTPS within development. This is done by creating a self-signed certificate. The self signed certificate is located in a keystore at `backend/src/main/resources/keystore/blokat.p12`. When the server is started, the self signed certificate should be found by the server.

Not just the backend is secured with HTTPS, but the frontend as well. This is done by setting the `start` script which is defined in `package.json` from `ng serve` to `ng serve --ssl`. When starting the server, ignore the security warning that the browser will give.

You can create a certificate by running following command. Make sure to use `***REMOVED***` as the password for the keystore, or if another password is used, change the file `application.yml`.
``` shell script
keytool -genkeypair -alias blokat -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore blokat.p12 -validity 3650
```


**Commands**
```shell script
./gradlew clean bootRunDev
```
or for windows
```shell script
gradlew.bat clean bootRunDev
```

**Remarks**
* `database.properties`

This file contains all queries that the application uses, as well as all names of the columns. But, <b>do not change this file</b>. The file will be overwritten if someone executes `update_database_properties.bat` or `update_database_properties.sh`.

For development convenience of SQL queries, the application queries are provided in the file `backend/database/application_queries.sql`. Please do read the explanatory comment before writing any queries.

If a table has been added, or a column name has changed, update the file `backend/database/application_columns.txt`.

When all updates have been made, execute one of the scripts `update_database_properties.bat` or `update_database_properties.sh`, depending on the OS you are using. Now, the `database.properties` file should be updated accordingly.



### Frontend
**Requirements**
- Node
- Angular

**Configure**
**Commands**
```shell
npm install # Install the dependencies
ng serve -o # Compile source files and open project in browser
```

### Profiling with Skywalking
**Requirements**
- Docker
- docker-compose

**Commands**
```shell
cd skywalking
docker-compose -f skywalking.yml up --build
```


De backend zal hier automatisch op connecteren aan de hand van een agent. Op poort `:8000` kun je de skywalking ui vinden.



## Production

So, you want to deploy this app in production? (studieplekken.ugent.be)

1. SSH to root@fireball.ugent.be
2. `cd blokat`
3. `git pull` (If there are changes you'll need to stash, then pull, then pop. Possibly fixing any conflicts)
3. `./deploy.sh`
