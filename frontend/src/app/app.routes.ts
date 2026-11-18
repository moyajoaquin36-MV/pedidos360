import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { adminGuard } from './auth/admin.guard';
import { panelGuard } from './auth/panel.guard';
import { LoginComponent } from './pages/login/login.component';
import { HomeComponent } from './pages/home/home.component';
import { AdminComponent } from './pages/admin/admin.component';
import { RegistroComponent } from './pages/registro/registro.component';
import { PanelComponent } from './pages/panel/panel.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: LoginComponent },
  { path: 'registro', component: RegistroComponent },
  { path: 'home', component: HomeComponent, canActivate: [MsalGuard] },
  { path: 'admin', component: AdminComponent, canActivate: [MsalGuard, adminGuard] },
  { path: 'panel', component: PanelComponent, canActivate: [MsalGuard, panelGuard] },
  { path: '**', redirectTo: 'login' }
];
