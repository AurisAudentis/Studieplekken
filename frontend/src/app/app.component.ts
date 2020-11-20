import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {TranslateService} from '@ngx-translate/core';
import {ApplicationTypeFunctionalityService} from './services/functionality/application-type/application-type-functionality.service';
import {AuthenticationService} from './services/authentication/authentication.service';
import {UserService} from './services/api/users/user.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  showLogin = true;
  showDashboard = true;
  showProfile = false;
  showScan = false;
  showManagement = false;
  showInformation = true;

  loggedIn = false;

  constructor(private translate: TranslateService,
              private router: Router,
              private functionalityService: ApplicationTypeFunctionalityService,
              private authenticationService: AuthenticationService,
              private userService: UserService) {
    /******************************
     *   Language support setup   *
     ******************************/
    // if you think about supporting another language, you must change the exported variable 'appLanguages'
    // in environments.ts accordingly. This variable is used in PenaltiesComponent to show the correct description

    translate.setDefaultLang('en');
    // tries to set the language to the default browserlanguage of the user if 'en' or 'nl' (else en)
    const browserLang = translate.getBrowserLang();

    if (localStorage.getItem('selectedLanguage') !== null) {
      translate.use(localStorage.getItem('selectedLanguage'));
    } else {
      // add another language? -> add language to regex and read comments at the beginning of this constructor!
      translate.use(browserLang.match(/en|nl/) ? browserLang : 'en');
      localStorage.setItem('selectedLanguage', 'en');
    }
  }

  ngOnInit(): void {
    // Upon successful login, the backend redirects the user to /dashboard.
    // Since the user was redirected to the cas-login website, the AppComponent
    // will be recreated. After the recreation, we try to log in in the frontend.
    this.authenticationService.login();

    // subscribe to the user observable to make sure that the correct information
    // is shown in the application.
    this.authenticationService.user.subscribe(
      next => {
        const scanFunc = this.functionalityService.showScanningFunctionality();

        // first, check if the user is logged in
        if (this.authenticationService.isLoggedIn()) {
          this.loggedIn = true;

          this.showLogin = false;
          this.showDashboard = true;
          this.showProfile = true;
          this.showInformation = true;

          // if the user is an admin, no extra request to the backend
          // is required to determine whether or not the user has authorities
          // and thus can be seen as an employee
          if (next.admin) {
            this.showScan = scanFunc && true;
            this.showManagement = true;
          } else {
            this.userService.hasUserAuthorities(next.augentID).subscribe(
              next2 => {
                this.showScan = scanFunc && next2;
                this.showManagement = next2;
              }
            );
          }

        } else {
          this.loggedIn = false;

          this.showLogin = true;
          this.showDashboard = true;
          this.showProfile = false;
          this.showScan = false;
          this.showManagement = false;
          this.showInformation = true;
        }

        // possibly overrule the shows above due to limitations in functionality
        this.showScan = this.functionalityService.showScanningFunctionality();
      }
    );
  }

  currentLanguage(): string {
    return localStorage.getItem('selectedLanguage');
  }

  otherSupportedLanguage(): string {
    return localStorage.getItem('selectedLanguage') === 'nl' ? 'en' : 'nl';
  }

  changeLanguage(event: Event): void {
    event.preventDefault();
    if (localStorage.getItem('selectedLanguage') === 'nl') {
      localStorage.setItem('selectedLanguage', 'en');
      this.translate.use('en');
    } else {
      localStorage.setItem('selectedLanguage', 'nl');
      this.translate.use('nl');
    }
  }

  isActive(path: string): boolean {
    return this.router.url === path;
  }

  logout(): void {
    this.authenticationService.logout();
  }
}
