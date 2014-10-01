describe("Test for drop-down-text-combo", function() {
  beforeEach(module(PKG.name + '.directives'));

  var $compile, scope, element;
  beforeEach(inject(function($rootScope, $compile){
    element = angular.element(
      '<drop-down-text-combo ' +
            'text-value="textValue" ' +
            'drop-down-value="dropDownValue" ' +
            'drop-down-options="dropDownOptions" ' +
            'drop-down-disable="isDropDownDisabled" ' +
            '> '+
        '</drop-down-text-combo>');
    scope = $rootScope.$new();

    scope.textValue = "test1";
    scope.dropDownOptions = [
      {
        name: "Something",
        description: "weird"
      },
      {
        name: "Something else",
        description: "Super normal"
      }
    ];
    scope.dropDownValue = "Something else";
    scope.isDropDownDisabled = "false";

    $compile(element)(scope);
    scope.$digest();
  }));

  it("should render properly with default drop down", function() {
    expect(element.find("select").val()).toEqual("1");
  });

  it("should render properly with change in drop down", function() {
    scope.$apply(function() {
      scope.dropDownValue = "Something";
    });
    expect(element.find("select").val()).toEqual("0");
  });

  it("should render properly with change in selected option", function() {
    scope.dropDownOptions.push({
      name: "sola",
      description: "dorima"
    });

    scope.dropDownOptions.push({
      name: "hola",
      description: "horima"
    });

    element.find("select").controller('ngModel').$setViewValue("sola");
    scope.$digest();
    expect(scope.dropDownValue).toEqual("sola");
    expect(element.find("select").val()).toEqual("2");
  });
  it("should populate the text value properly", function() {
    expect(element.find("input").val()).toEqual("test1");
  });
  it("should not change when disabled", function() {
    scope.dropDownOptions.push({
      name: "sola",
      description: "dorima"
    });

    scope.dropDownOptions.push({
      name: "hola",
      description: "horima"
    });
    scope.dropDownValue = "hola";
    scope.isDropDownDisabled = "true";
    element.find("select").controller('ngModel').$setViewValue("sola");
    scope.$digest();
    expect(scope.dropDownValue).toNotEqual("sola");
    expect(element.find("select").val()).toNotEqual("2");
  });
});
