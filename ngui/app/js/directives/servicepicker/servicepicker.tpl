<ul class="list-inline">
  <li ng-repeat="svc in model">
    <div class="btn-group">
      <a class="btn btn-xs btn-default disabled">
        {{svc}}
      </a>
      <a class="btn btn-xs btn-default" ng-click="rmService(svc)" ng-if="svc!=='base' && allowrm">
        <span class="fa fa-minus"></span>
      </a>
    </div>
  </li>
  <li ng-show="allowadd">
    <a class="btn btn-xs btn-warning" 
      ng-disabled="!dropdown.length"
      bs-dropdown="dropdown">
        <span class="fa fa-plus"></span>
        add service
    </a>
  </li>
</ul>