import { Injectable } from '@angular/core';

import { JsonService } from './json.service';
import { NotificationService } from './notification.service';

import { FijiMenuItem } from '../fiji-menu-item';

@Injectable()
export class MenuService {

  private menuUrl = 'http://localhost:8080/admin/menuNew';

  constructor(
    private jsonService: JsonService,
    private notificationService: NotificationService) {  }

  fetchMenu(): void {
    const service = this;
    this.jsonService.getJsonData(this.menuUrl)
      .subscribe((results: Object) => {
        service.notificationService.menuRootRetrieved(service.extractMenu(results));
    });
  }

  private extractMenu(menuItemCandidate: Object): FijiMenuItem {
    const menuItem: FijiMenuItem = new FijiMenuItem(
      menuItemCandidate['Level'],
      menuItemCandidate['Label'],
      menuItemCandidate['Command']);
    for (const key of Object.keys(menuItemCandidate).filter(prop => prop !== 'Level' && prop !== 'Label' && prop !== 'Command')) {
      menuItem.AddChild(this.extractMenu(menuItemCandidate[key]));
    }
    return menuItem;
  }
}
