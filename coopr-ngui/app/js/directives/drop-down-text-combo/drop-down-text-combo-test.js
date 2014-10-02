/*global module, expect, inject, describe, it, before, beforeEach, after, afterEach */

describe("Unit tests for drop-down-combo-text directive", function(myApi) {
  beforeEach(module(PKG.name + ".directives"));

  var scope, element, compile;
  beforeEach(inject(function($rootScope, $compile){
    compile = $compile;
    scope = $rootScope.$new();
    element = angular.element(
      "<drop-down-text-combo " +
            "data-model=\"model\"" +
            "data-drop-down-list=\"dropDownList\"" +
            "data-text-fields = \"textFields\"" +
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
    scope.textFields = [{
      name: "text1",
      placeholder: "somerandomplaceholder"
    }];
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

  it("should have valid placeholder for text field", function() {
    // Want to query an element inside angular's unit test?
    // Well, no you cannot have it easy.
    // Struggle like galileo who tried proving earth is not at the center of the solar system.
    expect(
      angular.element(
        element[0].querySelector("input[name=text1]")
      ).attr("placeholder")
    ).toEqual("somerandomplaceholder");
  });
});
