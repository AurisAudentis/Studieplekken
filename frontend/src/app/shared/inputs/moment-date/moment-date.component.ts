import { Component, EventEmitter, Input, OnChanges, OnInit, Output } from '@angular/core';
import * as moment from 'moment';
import { Moment } from 'moment';

@Component({
  selector: 'app-moment-date',
  templateUrl: './moment-date.component.html',
  styleUrls: ['./moment-date.component.css']
})
export class MomentDateComponent implements OnInit, OnChanges {

  @Input()
  model: Moment;
  @Input()
  type: 'date'|'time';
  @Output()
  modelChange: EventEmitter<Moment> = new EventEmitter();
  @Input()
  min: Moment;

  modelAsString: string;
  minAsString: string;

  constructor() { }

  ngOnInit(): void {
  }

  ngOnChanges(): void {
    if (this.model) {
      this.modelAsString = this.type === 'date' ? this.model.format('YYYY-MM-DD') : this.model.format('HH:mm');
    } else {
      this.modelAsString = '';
    }

    if (this.min) {
      this.minAsString = this.type === 'date' ? this.min.format('YYYY-MM-DD') : this.min.format('HH:mm:ss');
    } else {
      this.minAsString = '';
    }
  }

  onNewDate(): void {
    this.modelChange.next(moment(this.modelAsString, this.type === 'date' ? 'YYYY-MM-DD' : 'HH:mm'));
  }

}
