import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { ModalModule } from 'ngx-bootstrap/modal';
import { BsDropdownModule } from 'ngx-bootstrap';

import { AppComponent } from './app.component';
import { MenuComponent } from './menu.component';
import { MenuItemComponent } from './menu-item.component';
import { ModalContentComponent } from './modal-dialog-dynamic-components/modal.component';

import { AppRoutingModule } from './app-routing.module';

import { ObjectService } from './services/object.service';
import { ModuleService } from './services/module.service';
import { MenuService } from './services/menu.service';
import { JsonService } from './services/json.service';
import { NotificationService } from './services/notification.service';

@NgModule({
  imports: [
    BrowserModule,
    HttpClientModule,
    AppRoutingModule,
    FormsModule,
    ModalModule.forRoot(),
    BsDropdownModule.forRoot()
  ],
  declarations: [
    AppComponent,
    MenuComponent,
    MenuItemComponent,
    ModalContentComponent
  ],
  providers: [
    ModuleService,
    ObjectService,
    MenuService,
    JsonService,
    NotificationService
  ],
  entryComponents: [
    ModalContentComponent
  ],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule { }
