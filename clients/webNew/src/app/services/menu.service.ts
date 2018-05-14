import { Injectable } from '@angular/core';

import { JsonService } from './json.service';
import { NotificationService } from './notification.service';

import { FijiMenuItem } from '../fiji-menu-item';

@Injectable()
export class MenuService {

  private menuUrl = 'http://localhost:8080/modules/command:net.imagej.server.utilities.MenuProvider';

  constructor(
    private jsonService: JsonService,
    private notificationService: NotificationService) {  }

  fetchMenu(): void {
    const service = this;
    this.jsonService.postObject(this.menuUrl, new Object())
      .subscribe((results: Object) => {
        service.notificationService.menuRootRetrieved(service.extractMenu(results['mappedMenuItems']));
    });
  }

  private extractMenu(menuItemCandidate: Object): FijiMenuItem {
    const menuItem: FijiMenuItem = new FijiMenuItem(
      menuItemCandidate['Label'],
      menuItemCandidate['Command']);
    for (const key of Object.keys(menuItemCandidate).filter(prop => prop !== 'Label' && prop !== 'Command')) {
      menuItem.AddChild(this.extractMenu(menuItemCandidate[key]));
    }
    return menuItem;
  }
}
