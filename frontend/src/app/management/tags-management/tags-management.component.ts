import { Component, OnInit, TemplateRef } from '@angular/core';
import {
  AbstractControl,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { TagsService } from '../../services/api/tags/tags.service';
import { LocationTag } from '../../shared/model/LocationTag';

@Component({
  selector: 'app-tags-management',
  templateUrl: './tags-management.component.html',
  styleUrls: ['./tags-management.component.scss'],
})
export class TagsManagementComponent implements OnInit {
  tagsObs: Observable<LocationTag[]>;

  tagFormGroup = new UntypedFormGroup({
    tagId: new UntypedFormControl({ value: '', disabled: true }),
    dutch: new UntypedFormControl('', Validators.required.bind(this)),
    english: new UntypedFormControl('', Validators.required.bind(this)),
  });

  successGettingTags: boolean = undefined;
  successAddingTag: boolean = undefined;
  successUpdatingTag: boolean = undefined;
  successDeletingTag: boolean = undefined;

  constructor(
    private tagsService: TagsService,
    private modalService: MatDialog
  ) {}

  get tagId(): AbstractControl {
    return this.tagFormGroup.get('tagId');
  }

  get dutch(): AbstractControl {
    return this.tagFormGroup.get('dutch');
  }

  get english(): AbstractControl {
    return this.tagFormGroup.get('english');
  }

  get locationTag(): LocationTag {
    return {
      tagId: this.tagId.value as number,
      dutch: this.dutch.value as string,
      english: this.english.value as string,
    };
  }

  ngOnInit(): void {
    this.tagsObs = this.tagsService.getAllTags();
    this.tagsObs.subscribe(
      () => {
        // Setting the 'successGettingTags' to true doesn't really do anything
        this.successGettingTags = true;
      },
      () => {
        // But this does: gives feedback to the user
        this.successGettingTags = false;
      }
    );
  }

  prepareFormGroup(locationTag: LocationTag): void {
    this.tagFormGroup.setValue({
      tagId: locationTag.tagId,
      dutch: locationTag.dutch,
      english: locationTag.english,
    });
  }

  closeModal(): void {
    this.modalService.closeAll();
  }

  prepareAdd(template: TemplateRef<unknown>): void {
    // reset the feedback boolean
    this.successAddingTag = undefined;

    // prepare the tagFormGroup, note that the tagId won't be shown
    // because this is automatically added by the database
    this.tagFormGroup.setValue({
      tagId: -1, // tag id must be generated by backend
      dutch: '',
      english: '',
    });

    this.modalService.open(template, {panelClass: ["cs--cyan" ,"bigmodal"]});
  }

  addTag(): void {
    this.successAddingTag = null;
    this.tagsService.addTag(this.locationTag).subscribe(
      () => {
        this.successAddingTag = true;
        // and reload the tags
        this.tagsObs = this.tagsService.getAllTags();
        this.modalService.closeAll();
      },
      () => {
        this.successAddingTag = false;
      }
    );
  }

  prepareUpdate(
    locationTag: LocationTag,
    template: TemplateRef<unknown>
  ): void {
    // reset the feedback boolean
    this.successUpdatingTag = undefined;

    // prepare the tagFormGroup
    this.prepareFormGroup(locationTag);

    this.modalService.open(template,  {panelClass: ["cs--cyan" ,"bigmodal"]});
  }

  updateTagInFormGroup(): void {
    this.successUpdatingTag = null;
    this.tagsService.updateTag(this.locationTag).subscribe(
      () => {
        this.successUpdatingTag = true;
        // and reload the tags
        this.tagsObs = this.tagsService.getAllTags();
        this.modalService.closeAll();
      },
      () => {
        this.successUpdatingTag = false;
      }
    );
  }

  prepareToDelete(
    locationTag: LocationTag,
    template: TemplateRef<unknown>
  ): void {
    // reset the feedback boolean
    this.successDeletingTag = undefined;

    // prepare the tagFormGroup
    this.prepareFormGroup(locationTag);

    this.modalService.open(template,  {panelClass: ["cs--cyan" ,"bigmodal"]});
  }

  deleteTagInFormGroup(): void {
    this.successDeletingTag = null;
    this.tagsService.deleteTag(this.locationTag).subscribe(
      () => {
        this.successDeletingTag = true;
        // and reload the tags
        this.tagsObs = this.tagsService.getAllTags();
        this.modalService.closeAll();
      },
      () => {
        this.successDeletingTag = false;
      }
    );
  }

  validTagFormGroup(): boolean {
    return !this.tagFormGroup.invalid;
  }
}
