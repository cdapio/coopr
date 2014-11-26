'use strict';

describe('api resources', function() {
  beforeEach(module('coopr-ui.services'));

  describe('myApi', function() {
    var myApi, myAuth, MY_CONFIG, $rootScope, $httpBackend;

    beforeEach(inject(function($injector) {
      myApi = $injector.get('myApi');
      myAuth = $injector.get('myAuth');
      MY_CONFIG = $injector.get('MY_CONFIG');
      $rootScope = $injector.get('$rootScope');
      $httpBackend = $injector.get('$httpBackend');
    }));

    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    describe('Cluster', function() {

      it('is a $resource with custom methods', function() {
        expect(myApi.Cluster.query).toEqual(jasmine.any(Function));
        expect(myApi.Cluster.get).toEqual(jasmine.any(Function));
        expect(myApi.Cluster.getStatus).toEqual(jasmine.any(Function));
        expect(myApi.Cluster.startAllServices).toEqual(jasmine.any(Function));
      });

      it('instance has custom methods', function() {
        var cluster = new myApi.Cluster({id:123});
        expect(cluster.$getStatus).toEqual(jasmine.any(Function));
        expect(cluster.$startAllServices).toEqual(jasmine.any(Function));
        expect(cluster.$delete).toEqual(jasmine.any(Function));
      });

      it('testing query()', function() {
        $httpBackend.expectGET(/v2\/clusters$/).respond([{foo:'bar'}]);

        var list = myApi.Cluster.query({status:null});
        $httpBackend.flush();
        expect(list[0].foo).toEqual('bar');
      });

      it('testing get()', function() {
        $httpBackend.expectGET(/v2\/clusters\/123$/).respond({foo:'bar'});

        var item = new myApi.Cluster({id:123});
        item.$get();
        $httpBackend.flush();
        expect(item.foo).toEqual('bar');
      });

      it('should send Authorization header', function() {
        $httpBackend.expectGET(/v2\/clusters$/, function(headers) {
          return headers['Authorization'] === MY_CONFIG.authorization;
        }).respond(201, '');

        myApi.Cluster.query({status:null});
        $httpBackend.flush();
      });

      it('should send X-Requested-With header', function() {
        $httpBackend.expectGET(/v2\/clusters$/, function(headers) {
          return !!headers['X-Requested-With'];
        }).respond(201, '');

        myApi.Cluster.query({status:null});
        $httpBackend.flush();
      });

      it('should send Coopr headers when logged in', function() {
        $httpBackend.expectGET(/v2\/clusters$/, function(headers) {
          return headers['Coopr-UserID'] === 'test';
        }).respond(201, '');

        myAuth.currentUser = {username:'test'};
        myApi.Cluster.query({status:null});
        $httpBackend.flush();
        myAuth.currentUser = null;
      });
    });

  });


});
