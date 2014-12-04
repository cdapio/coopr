/*global module, expect, inject, describe, it, before, beforeEach, after, afterEach */

describe('directive myConfigtoformfields', function() {
  beforeEach(module(PKG.name + '.commons'));

  var $compile, scope;
  beforeEach(inject(function(_$compile_, $rootScope){
    $compile = _$compile_;
    scope = $rootScope.$new();
  }));


  it('without a config it renders nothing', function() {
    var el = $compile('<my-configtoformfields />')(scope);
    scope.$digest();

    expect(el.find('label').length).toEqual(0);
  });

  it('should watch the provided config and set the model', function() {
    var config = {
      fields: {
        foo: {
          label: 'foo label',
          override: true,
          tip: 'this is a tip',
          type: 'text',
          default: 'whatever'
        }
      }
    };

    scope.config = config;
    scope.model = {};

    var el = $compile('<my-configtoformfields data-model="model" data-config="config" data-allow-override="true" />')(scope);
    scope.$digest();

    expect(el.find('label').length).toEqual(1);
    expect(el.find('label').text()).toMatch(config.fields.foo.label);

    expect(el.find('input').attr('required')).toBeFalsy();

    expect(el.find('input').attr('placeholder')).toEqual(config.fields.foo.tip);
    expect(el.find('input').attr('disabled')).toBeFalsy();

    expect(scope.model.foo).toEqual(config.fields.foo.default);

    scope.$apply(function () {
      scope.config.fields.foo.tip = 'new tip';
      scope.config.fields.foo.override = false;
    });

    expect(el.find('input').attr('placeholder')).toEqual(scope.config.fields.foo.tip);
    expect(el.find('input').attr('disabled')).toBeTruthy();

  });

  it('should honor required array', function() {
    var config = {
      fields: {
        foo: {
          label: 'foo label',
          override: true,
          tip: 'this is a tip',
          type: 'text'
        }
      },
      required: [['bar']]
    };

    scope.config = config;
    scope.model = {};

    var el = $compile('<my-configtoformfields data-model="model" data-config="config" />')(scope);
    scope.$digest();

    expect(el.find('input').attr('required')).toBeFalsy();

    scope.$apply(function () {
      scope.config.required = [['foo', 'bar'], ['baz']];
    });
    expect(el.find('input').attr('required')).toBeFalsy();

    scope.$apply(function () {
      scope.model.foo = 'hello';
    });
    expect(el.find('input').attr('required')).toBeTruthy();

    scope.$apply(function () {
      scope.config.required = [];
    });
    expect(el.find('input').attr('required')).toBeFalsy();

    scope.$apply(function () {
      scope.model.foo = 'hello again';
    });
    expect(el.find('input').attr('required')).toBeFalsy();
  });





});
