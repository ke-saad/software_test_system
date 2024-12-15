import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home.component';
import { UploadFileComponent } from './upload-file/upload-file.component';

export const routes: Routes = [
    { path: '', component: HomeComponent },
    { path: 'upload', component: UploadFileComponent },
  ];