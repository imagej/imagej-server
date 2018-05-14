import { Component, OnInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';

import { ObjectService } from './services/object.service';
import { ModuleService } from './services/module.service';
import { MenuService } from './services/menu.service';
import { NotificationService } from './services/notification.service';

import { Subscription } from 'rxjs/Subscription';

import { FijiObject } from './fiji-object';
import { FijiModule } from './fiji-module';
import { FijiMenuItem } from './fiji-menu-item';

@Component({
  selector: 'app-component-menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css']
})

export class MenuComponent implements OnInit, OnDestroy {

  private subscriptions: Subscription[] = [];
  retrievedMenuRoot: FijiMenuItem;

  uploadedObjects: FijiObject[];
  activeObject: FijiObject = null;

  availableModules: FijiModule[];

  @ViewChild('fileInput') fileInputElementRef: ElementRef;

  constructor(
    private objectService: ObjectService,
    private moduleService: ModuleService,
    private menuService: MenuService,
    private notificationService: NotificationService) { }

  ngOnInit(): void {
    this.subscriptions.push(
      this.notificationService.objectsRetrievedNotification.subscribe(list => {
        this.handleRefreshedObjectList(list);
      }));
    this.subscriptions.push(
      this.notificationService.modulesRetrievedNotification.subscribe(list => {
        this.availableModules = list;
      }));
    this.subscriptions.push(
      this.notificationService.menuRootRetrievedNotification.subscribe((menuRoot: FijiMenuItem) => {
        this.retrievedMenuRoot = menuRoot;
      }));
    this.subscriptions.push(
      this.notificationService.menuItemClickedNotification.subscribe( (menuItem: FijiMenuItem) => {
        this.handleMenuSelection(menuItem);
      }));
    this.objectService.fetchObjects();
    this.moduleService.fetchModules();
    this.menuService.fetchMenu();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
  }

  handleRefreshedObjectList(list: FijiObject[]): void {
    if (this.activeObject !== null) {
      const updatedActiveObject = list.find(o => o.id === this.activeObject.id);
      if (updatedActiveObject !== null) {
        this.activeObject = updatedActiveObject;
      }
    }
    this.uploadedObjects = list;
  }

  private async handleMenuSelection(menuItem: FijiMenuItem) {
      const module: FijiModule = this.availableModules.find(m => m.rawName === 'command:' + menuItem.command);
      if (module === null) {
        alert('No corresponding module found!');
        return;
      }
      const observable = await this.moduleService.executeModule(module, this.activeObject, menuItem.label);
      observable.subscribe(outputs => {
        if (outputs !== null) {
          this.objectService.fetchObjects();
        }
      });
  }

  uploadImage() {
    const files = this.fileInputElementRef.nativeElement['files'];
    if (files.length !== 1) {
      alert('Need exactly one file to upload!');
      return;
    }
    this.objectService.uploadObject(files[0]);
  }

    setObjectActive(object: FijiObject) {
    this.activeObject = object;
  }
}
