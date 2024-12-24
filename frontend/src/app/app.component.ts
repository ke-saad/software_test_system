import {Component, CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {RouterModule} from '@angular/router';
import {HomeComponent} from './features/home/home.component';
import {UploadFileComponent} from './upload-file/upload-file.component';
import {NavbarComponent} from './core/navbar/navbar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterModule,
    HomeComponent,
    UploadFileComponent,
    NavbarComponent,
  ],
  template: `
    <app-navbar></app-navbar>
    <router-outlet></router-outlet>  <!-- Add router-outlet directive here -->
  `,
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppComponent {
  title = 'frontend';
}
