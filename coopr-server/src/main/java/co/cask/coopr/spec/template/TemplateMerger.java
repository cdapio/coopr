/*
 * Copyright © 2012-2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.cask.coopr.spec.template;

import co.cask.coopr.common.utils.StringUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * A template merger used for construction cluster templates from includes and parent extends.
 */
public class TemplateMerger {

  /**
   * Merge all templates in order into new cluster template. At the end we have cluster template with fully merged
   * state from included partials and extended parents.
   * @param templates templates in order to merge into new cluster template. Recommend to use
   *                  {@link java.util.LinkedHashSet} or {@link java.util.List} for saving templates order.
   * @param initialTemplate specified template, last child of templates chain. Name, description, label ans icon will
   *                        be taken from it for final cluster template.
   * @return Cluster template with merged state
   * @throws TemplateImmutabilityException if some template tries to override immutable config
   * @throws TemplateNotFoundException if template is unknown type.
   */
  public ClusterTemplate merge(Collection<AbstractTemplate> templates, ClusterTemplate initialTemplate)
    throws TemplateImmutabilityException, TemplateNotFoundException {
    Set<String> immutables = Sets.newHashSet();
    ClusterTemplate toTemplate = ClusterTemplate.builder()
      .setName(initialTemplate.getName())
      .setDescription(initialTemplate.getDescription())
      .setIcon(initialTemplate.getIcon())
      .setLabel(initialTemplate.getLabel())
      .build();
    toTemplate.includes = initialTemplate.includes;
    toTemplate.parent = initialTemplate.parent;
    for (AbstractTemplate fromTemplate : templates) {
      if (fromTemplate instanceof ClusterTemplate) {
        copyFullProps(fromTemplate, toTemplate, immutables);
      } else if (fromTemplate instanceof PartialTemplate) {
        Boolean isImmutable = ((PartialTemplate) fromTemplate).isImmutable();
        copyMainProps(fromTemplate, toTemplate, immutables, isImmutable);
      } else {
        throw new TemplateNotFoundException("Template not partial and not cluster.");
      }
    }
    validateFields(toTemplate);
    return toTemplate;
  }

  private void validateFields(AbstractTemplate to) {
    Preconditions.checkArgument(to.clusterDefaults.getServices() != null && !to.clusterDefaults.getServices().isEmpty(),
                                "default services must be specified");
    Preconditions.checkArgument(to.clusterDefaults.getProvider() != null, "default provider must be specified");
    Preconditions.checkArgument(to.clusterDefaults.getDnsSuffix() == null ||
                                  StringUtils.isValidDNSSuffix(to.clusterDefaults.getDnsSuffix()),
                                to.clusterDefaults.getDnsSuffix() + " is an invalid DNS suffix.");
  }

  //for partial includes
  private void copyMainProps(AbstractTemplate from, AbstractTemplate to, Set<String> immutables, boolean isImmutable)
    throws TemplateImmutabilityException {
    //merge defaults
    to.clusterDefaults.services = mergeSet(to.clusterDefaults.services, from.clusterDefaults.services);

    //merge defaults config
    JsonObject fromDefaultsConfig = from.getClusterDefaults().getConfig();
    for (Map.Entry<String, JsonElement> fromConfigItem : fromDefaultsConfig.entrySet()) {
      JsonObject thisConfig = to.clusterDefaults.getConfig();
      String name = fromConfigItem.getKey();
      JsonElement value = fromConfigItem.getValue();
      if (immutables.contains(name) && thisConfig.has(name)) {
        throw new TemplateImmutabilityException(name + " can't be overridden due immutability. Overrides in "
                                                  + from.getName());
      }
      thisConfig.add(name, value);
      if (isImmutable) {
        immutables.add(name);
      }
    }
    //merge compatibilities services
    to.compatibilities.services = mergeSet(to.compatibilities.services, from.compatibilities.services);
  }

  //for parent extension
  private void copyFullProps(AbstractTemplate from, AbstractTemplate to, Set<String> immutables)
    throws TemplateImmutabilityException {
    copyMainProps(from, to, immutables, false);

    //merge defaults options
    ClusterDefaults fromDefaults = from.getClusterDefaults();
    if(isNotBlank(fromDefaults.getDnsSuffix())) to.clusterDefaults.setDnsSuffix(fromDefaults.getDnsSuffix());
    if(isNotBlank(fromDefaults.getHardwaretype())) to.clusterDefaults.setHardwaretype(fromDefaults.getHardwaretype());
    if(isNotBlank(fromDefaults.getImagetype())) to.clusterDefaults.setImagetype(fromDefaults.getImagetype());
    if(isNotBlank(fromDefaults.getProvider())) to.clusterDefaults.setProvider(fromDefaults.getProvider());

    //merge constraints
    to.constraints.serviceConstraints = mergeMap(to.constraints.serviceConstraints, from.constraints.serviceConstraints);
    to.constraints.layoutConstraint.servicesThatMustCoexist = mergeSet(to.constraints.layoutConstraint.servicesThatMustCoexist,
             from.constraints.layoutConstraint.servicesThatMustCoexist);
    to.constraints.layoutConstraint.servicesThatMustNotCoexist = mergeSet(to.constraints.layoutConstraint.servicesThatMustNotCoexist,
               from.constraints.layoutConstraint.servicesThatMustNotCoexist);
    if (!from.constraints.sizeConstraint.equals(SizeConstraint.EMPTY)) {
      to.constraints.sizeConstraint = from.constraints.sizeConstraint;
    }

    //merge compatibilities
    to.compatibilities.hardwaretypes = mergeSet(to.compatibilities.hardwaretypes, from.compatibilities.hardwaretypes);
    to.compatibilities.imagetypes = mergeSet(to.compatibilities.imagetypes, from.compatibilities.imagetypes);
    to.compatibilities.services = mergeSet(to.compatibilities.services, from.compatibilities.services);

    //merge admin lease duration
    if (!from.administration.leaseDuration.equals(LeaseDuration.FOREVER_LEASE_DURATION)) {
      to.administration.leaseDuration = from.administration.leaseDuration;
    }
  }

  private <T> Set<T> mergeSet(Set<T> first, Set<T> second) {
    if (second != null) {
      return ImmutableSet.<T>builder().addAll(first).addAll(second).build();
    } else {
      return first;
    }
  }

  private <T, V> Map<T, V> mergeMap(Map<T, V> first, Map<T, V> second) {
    if (second != null) {
      //ImmutableMap.builder does not applies duplicates, so forced to use this
      HashMap<T, V> tempMap = new HashMap<T, V>();
      tempMap.putAll(first);
      tempMap.putAll(second);
      return ImmutableMap.copyOf(tempMap);
    } else {
      return first;
    }
  }
}