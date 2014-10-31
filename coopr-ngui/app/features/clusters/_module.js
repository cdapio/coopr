angular.module(PKG.name+'.feature.clusters', [
  PKG.name+'.services',
  PKG.name+'.filters',
  PKG.name+'.directives',

  PKG.name+'.feature.crud',

  'ui.router',
  'mgcrea.ngStrap.alert',
  'mgcrea.ngStrap.modal',
  'mgcrea.ngStrap.tooltip',
  'mgcrea.ngStrap.popover',
  'mgcrea.ngStrap.button',

  'angularMoment'

]);