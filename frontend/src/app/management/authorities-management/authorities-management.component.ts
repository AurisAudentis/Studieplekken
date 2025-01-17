import { Component, OnInit, TemplateRef } from '@angular/core';
import {
  AbstractControl,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { AuthoritiesService } from '../../services/api/authorities/authorities.service';
import { AuthorityToManageService } from '../../services/single-point-of-truth/authority-to-manage/authority-to-manage.service';
import { Authority } from '../../shared/model/Authority';

@Component({
  selector: 'app-authorities-management',
  templateUrl: './authorities-management.component.html',
  styleUrls: ['./authorities-management.component.scss'],
})
export class AuthoritiesManagementComponent implements OnInit {
  authoritiesObs: Observable<Authority[]>;

  authorityFormGroup = new UntypedFormGroup({
    authorityId: new UntypedFormControl({ value: '', disabled: true }),
    authorityName: new UntypedFormControl('', Validators.required.bind(this)),
    description: new UntypedFormControl('', Validators.required.bind(this)),
  });

  successGettingAuthorities: boolean = undefined;
  successAddingAuthority: boolean = undefined;
  successUpdatingAuthority: boolean = undefined;
  successDeletingAuthority: boolean = undefined;

  constructor(
    private authoritiesService: AuthoritiesService,
    private authorityToMangeService: AuthorityToManageService,
    private modalService: MatDialog
  ) { }

  // *****************
  // *   Auxiliary   *
  // *****************/

  get authorityId(): AbstractControl {
    return this.authorityFormGroup.get('authorityId');
  }

  get authorityName(): AbstractControl {
    return this.authorityFormGroup.get('authorityName');
  }

  get description(): AbstractControl {
    return this.authorityFormGroup.get('description');
  }

  get authority(): Authority {
    return {
      authorityId: this.authorityId.value as number,
      authorityName: this.authorityName.value as string,
      description: this.description.value as string,
    };
  }

  ngOnInit(): void {
    this.authoritiesObs = this.authoritiesService.getAllAuthorities();

    this.successGettingAuthorities = null;
    this.authoritiesObs.subscribe(
      () => {
        // Setting the 'successGettingAuthorities' to true doesn't really do anything
        this.successGettingAuthorities = true;
      },
      () => {
        // But this does: gives feedback to the user
        this.successGettingAuthorities = false;
      }
    );
  }

  prepareFormGroup(authority: Authority): void {
    this.authorityFormGroup.setValue({
      authorityId: authority.authorityId,
      authorityName: authority.authorityName,
      description: authority.description,
    });
  }

  /**
   * Close whatever modal is opened
   */
  closeModal(): void {
    this.modalService.closeAll();
  }

  // ********************
  // *   CRUD: Create   *
  // ********************/

  prepareAdd(template: TemplateRef<unknown>): void {
    // reset the feedback boolean
    this.successAddingAuthority = undefined;

    // prepare the authorityFormGroup, note that the authorityId won't be shown
    // because this is automatically added by the database
    this.authorityFormGroup.setValue({
      authorityId: -1, // authority id must be generated by backend
      authorityName: '',
      description: '',
    });

    this.modalService.open(template, { panelClass: ["cs--cyan", "bigmodal"] });
  }

  addAuthority(): void {
    this.successAddingAuthority = null;
    this.authoritiesService.addAuthority(this.authority).subscribe(
      () => {
        this.successAddingAuthority = true;
        // and reload the tags
        this.authoritiesObs = this.authoritiesService.getAllAuthorities();
        this.modalService.closeAll();
      },
      () => {
        this.successAddingAuthority = false;
      }
    );
  }

  // ********************
  // *   CRUD: Update   *
  // ********************/

  prepareUpdate(authority: Authority, template: TemplateRef<unknown>): void {
    // reset the feedback boolean
    this.successUpdatingAuthority = undefined;

    // prepare the tagFormGroup
    this.prepareFormGroup(authority);
    this.modalService.open(template, { panelClass: ["cs--cyan", "bigmodal"] });
  }

  updateTagInFormGroup(): void {
    this.successUpdatingAuthority = null;
    this.authoritiesService
      .updateAuthority(this.authorityId.value, this.authority)
      .subscribe(
        () => {
          this.successUpdatingAuthority = true;
          // and reload the tags
          this.authoritiesObs = this.authoritiesService.getAllAuthorities();
          this.modalService.closeAll();
        },
        () => {
          this.successUpdatingAuthority = false;
        }
      );
  }

  // ********************
  // *   CRUD: Delete   *
  // ********************/

  prepareToDelete(authority: Authority, template: TemplateRef<unknown>): void {
    // reset the feedback boolean
    this.successDeletingAuthority = undefined;

    // prepare the tagFormGroup
    this.prepareFormGroup(authority);
    this.modalService.open(template, { panelClass: ["cs--cyan", "bigmodal"] });
  }

  deleteTagInFormGroup(): void {
    this.successDeletingAuthority = null;
    this.authoritiesService
      .deleteAuthority(this.authority.authorityId)
      .subscribe(
        () => {
          this.successDeletingAuthority = true;
          // and reload the tags
          this.authoritiesObs = this.authoritiesService.getAllAuthorities();
          this.modalService.closeAll();
        },
        () => {
          this.successDeletingAuthority = false;
        }
      );
  }

  validTagFormGroup(): boolean {
    return !this.authorityFormGroup.invalid;
  }

  setAuthorityToManage(authority: Authority): void {
    this.authorityToMangeService.authority = authority;
  }
}
