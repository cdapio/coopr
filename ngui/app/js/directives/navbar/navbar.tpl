<div class="navbar-header">
  <button type="button" class="navbar-toggle" ng-click="navbarCollapsed = !navbarCollapsed">
    <span class="sr-only">Toggle navigation</span>
    <span class="icon-bar"></span>
    <span class="icon-bar"></span>
    <span class="icon-bar"></span>
  </button>
  <a class="navbar-brand" href="/">
     Coopr
   </a>
</div>
<div class="collapse navbar-collapse" ng-class="{in:navbarCollapsed}">

  <div ng-class="{bigicons: currentUser.hasRole('admin')}" ng-click="navbarCollapsed = false">
    <ul class="nav navbar-nav" ng-show="currentUser.hasRole('admin')">
      <li ng-repeat="link in navbarAdminLinks" ng-class="{active: $state.includes(link.sref)}">
        <a ui-sref="{{link.sref + '.list'}}">
          <span ng-class="link.icon && 'fa fa-' + link.icon"></span>
          {{link.label}}
        </a>
      </li>
    </ul>

    <ul class="nav navbar-nav" ng-show="currentUser">
      <li ng-class="{active: $state.includes('clusters')}">
        <a ui-sref="clusters.list">
          <span class="fa fa-cubes"></span>
          Clusters
        </a>
      </li>
    </ul>

  </div>

  <ul class="nav navbar-nav navbar-right">

    <li class="dropdown">
      <a href="" class="dropdown-toggle">
        <span ng-show="currentUser">
          <span class="fa fa-user"></span>
          {{currentUser.username | myCapitalizeFilter}}
        </span>
        <span ng-hide="currentUser">
          Not authenticated
        </span>
        <span class="caret"></span>
      </a>
    </li>

  </ul>
</div>