/*global module, expect, inject, describe, it, before, beforeEach, after, afterEach */

describe("Unit tests for my-dropdown-combo-text directive", function(myApi) {
  beforeEach(module(PKG.name + ".directives"));

  var scope, directiveScope, element;
  beforeEach(inject(function($rootScope, $compile) {
    element = angular.element(
      "<my-dropdown-text-combo " +
            "data-model=\"model\"" +
            "data-dropdown-list=\"dropDownList\"" +
            "data-text-fields = \"textFields\"" +
            "data-asset-label=\"Provider\"" +
            "> "+
        "</my-dropdown-text-combo>");
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

    directiveScope = element.isolateScope();

  }));

  it("Idealistic case where all data is perfect", function() {
    expect(directiveScope.dropdownValues.length).toBe(1);
  });


  it("should work with change in model", function() {
    scope.model.field3 = {
      flavor: "field3"
    };
    scope.$digest();
    expect(directiveScope.dropdownValues.length).toBe(0);
  });

  it("should work with deletion in model", function() {
    delete scope.model.field3;
    delete scope.model.something;
    scope.$digest();
    expect(directiveScope.dropdownValues.length).toBe(2);
  });

  it("should have valid placeholder for text field", function() {
    // Want to query an element inside angular's unit test?
    // Well, no you cannot have it easy.
    // Struggle like galileo who tried proving earth is not at the center of the solar system.
    expect(
      element[0].querySelector("input[name=text1]").getAttribute('placeholder')
    ).toEqual("somerandomplaceholder");
  });
});
