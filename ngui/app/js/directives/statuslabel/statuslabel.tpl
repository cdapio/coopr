<span class="label" ng-class="{

  complete: 'label-success',
  running: 'label-warning',
  active: 'label-info', 
  pending: 'label-info',
  incomplete: 'label-danger',
  failed: 'label-danger'

}[status] || 'label-default'">
  {{status}}
</span>

