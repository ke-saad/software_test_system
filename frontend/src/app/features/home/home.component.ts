import {Component, OnInit} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {ActivatedRoute, RouterModule} from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MatButtonModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css'],
})
export class HomeComponent implements OnInit {
  constructor(private route: ActivatedRoute) {
  }

  ngOnInit(): void {

    this.route.params.subscribe(params => {

    });
  }
}
