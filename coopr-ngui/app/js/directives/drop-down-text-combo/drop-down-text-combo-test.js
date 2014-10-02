/*global module, expect, inject, describe, it, before, beforeEach, after, afterEach */

describe("Unit tests for drop-down-combo-text directive", function(myApi) {
  beforeEach(module(PKG.name + ".directives"));

  var scope, element, compile;
  beforeEach(inject(function($rootScope, $compile){
    compile = $compile;
    scope = $rootScope.$new();
    element = angular.element(
      "<drop-down-text-combo " +
            "ng-model=\"model\"" +
            "drop-down-list=\"dropDownList\"" +
            "text-fields = \"textFields\"" +
            "> "+
        "</drop-down-text-combo>");
    scope = $rootScope.$new();
    scope.model = {
      "something": {
        "flavor": "value"
      },
      "somethingelse": {
        "flavor": "value"
      }
    };
    scope.textFields = ["text1"];
    scope.dropDownList = [
      {
        name: "something",
        value: "asadsa"
      },
      {
        name: "somethingelse",
        value: "asadsa"
      },
      {
        name: "field3",
        value: "asadsa"
      }];


    $compile(element)(scope);
    scope.$digest();
  }));

  it("Idealistic case where all data is perfect", function() {
    var localScope = element.isolateScope();
    expect(localScope.dropDownValues.length).toBe(1);
  });
  it("should work with change in model", function() {
    scope.model.field3 = {
      flavor: "field3"
    };
    scope.$digest();
    expect(element.isolateScope().dropDownValues.length).toBe(0);
  });

  it("should work with deletion in model", function() {
    delete scope.model.field3;
    delete scope.model.something;
    scope.$digest();
    expect(element.isolateScope().dropDownValues.length).toBe(2);
  });
});
