/*global jasmine, module, expect, inject, describe, it, before, beforeEach, after, afterEach */

describe('login feature', function(){
  beforeEach(module('coopr-ui.features'));

  describe('LoginCtrl', function() {
    var $scope;

    beforeEach(inject(function($rootScope, $controller) {
      $scope = $rootScope.$new();
      $controller('LoginCtrl', {$scope: $scope});
    }));

    it('should init credentials', function() {
      expect($scope.credentials).toBeDefined();
    });

    it('has a doLogin method', function() {
      expect($scope.doLogin).toEqual(jasmine.any(Function));
    });

  });


});
