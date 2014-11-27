/*global jasmine, module, expect, inject, describe, it, before, beforeEach, after, afterEach */

describe('crud feature', function(){
  beforeEach(module('coopr-ui.feature.crud'));


  describe('CrudCreateCtrl', function() {
    var $scope, myApi;

    beforeEach(inject(function($rootScope, $controller, _myApi_) {
      $scope = $rootScope.$new();
      myApi = _myApi_;
      $controller('CrudCreateCtrl', {$scope: $scope, $state: {
        current: { data: {modelName:'Cluster'} }
      }});
    }));

    it('has a doSubmit method', function() {
      expect($scope.doSubmit).toEqual(jasmine.any(Function));
    });

    it('model is a *modelName* instance', function() {
      expect($scope.model).toEqual(jasmine.any(myApi.Cluster));
    });

  });

});
