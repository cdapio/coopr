'use strict';

describe('directive', function() {
  beforeEach(module('coopr-ngui.directives'));

  var $compile, scope;
  beforeEach(inject(function(_$compile_, $rootScope){
    $compile = _$compile_;
    scope = $rootScope.$new();
  }));


  describe('myProgress', function() {

    describe('type=bar', function() {

      it('should render a progress-bar', function() {
        var el = $compile('<my-progress type="bar" />')(scope);
        scope.$digest();

        expect(el.hasClass('progress')).toBe(true);
        expect(el.children().hasClass('progress-bar')).toBe(true);
      });

      it('add-cls attributes decoration', function() {
        var el = $compile('<my-progress type="bar" data-add-cls="danger" />')(scope);
        scope.$digest();

        expect(el.children().hasClass('progress-bar-danger')).toBe(true);
      });


      it('sets the percentage', function() {
        var el = $compile('<my-progress type="bar" value="5" max="10" />')(scope);
        scope.$digest();

        expect(el.children().css('width')).toBe('50%');
        expect(el.children().text()).toMatch('50%');
      });

    });

  });


});
