import { Component, Input } from '@angular/core';
import { FijiMenuItem } from './fiji-menu-item';

import { NotificationService } from './services/notification.service';

@Component({
  selector: 'app-component-menu-item',
  styleUrls: ['./menu-item.component.css'],
  templateUrl: './menu-item.component.html'
})

export class MenuItemComponent {

  @Input() menuItem: FijiMenuItem;

  constructor(private notificationService: NotificationService) {  }

  updateListeners() {
    this.notificationService.menuItemClicked(this.menuItem);
  }
}
