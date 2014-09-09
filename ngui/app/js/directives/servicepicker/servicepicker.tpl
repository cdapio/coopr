<ul class="list-inline">
  <li ng-repeat="svc in model">
    <div class="btn-group btn-group-xs">
      <a class="btn btn-default disabled">
        {{svc}}
      </a>
      <a class="btn btn-default dropdown-toggle" ng-if="svc!=='base'" bs-dropdown="actionDropdowns[svc]" data-html="true">
        <span class="caret"></span>
      </a>
    </div>
  </li>
  <li ng-show="allowAdd">
    <a class="btn btn-xs btn-warning" 
      ng-disabled="!addsvcDropdown.length"
      bs-dropdown="addsvcDropdown">
        <span class="fa fa-plus"></span>
        add service
    </a>
  </li>
</ul>