import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { provideHttpClient } from '@angular/common/http'; // Import provideHttpClient
import { provideRouter } from '@angular/router'; // Import provideRouter
import { routes } from './app/app.routes'; // Import the routes

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(),    // Use HttpClient provider
    provideRouter(routes)    // Register the router with the routes
  ]
}).catch((err) => console.error(err));
