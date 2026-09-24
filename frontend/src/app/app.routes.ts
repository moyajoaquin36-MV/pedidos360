import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { adminGuard } from './auth/admin.guard';
import { LoginComponent } from './pages/login/login.component';
import { HomeComponent } from './pages/home/home.component';
import { AdminComponent } from './pages/admin/admin.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: LoginComponent },
  { path: 'home', component: HomeComponent, canActivate: [MsalGuard] },
  { path: 'admin', component: AdminComponent, canActivate: [MsalGuard, adminGuard] },
  { path: '**', redirectTo: 'login' }
];
