/**
 * [TODO] Add some description.
 */

var module = angular.module(PKG.name+'.directives');

module.directive('myNavbar', function myNavbarDirective ($dropdown, myAuth, myTheme) {
  return {
    restrict: 'A',
    templateUrl: 'navbar/navbar.html',

    link: function (scope, element, attrs) {

      $dropdown(angular.element(element[0].querySelector('a.dropdown-toggle')), {
        template: 'navbar/dropdown.html',
        animation: 'am-flip-x',
        placement: 'bottom-right',
        scope: scope
      });

      scope.logout = myAuth.logout;

      scope.theme = myTheme;

      scope.navbarAdminLinks = [
        { sref: 'templates',     label: 'Catalog',   icon: 'folder-open-o' },
        { sref: 'providers',     label: 'Providers', icon: 'cloud-upload'  },
        { sref: 'hardwaretypes', label: 'Hardware',  icon: 'cube'          },
        { sref: 'imagetypes',    label: 'Images',    icon: 'image'         },
        { sref: 'services',      label: 'Services',  icon: 'cog'           }
      ];

    }
  };
});
