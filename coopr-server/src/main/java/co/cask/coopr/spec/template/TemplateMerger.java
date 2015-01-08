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

import static org.apache.commons.lang.StringUtils.isBlank;

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
    throws TemplateImmutabilityException, TemplateNotFoundException, TemplateValidationException {
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
    try {
      validateFields(toTemplate);
    } catch (IllegalArgumentException e){
      throw new TemplateValidationException(e.getMessage(), e);
    }

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
    if(!from.clusterDefaults.equals(ClusterDefaults.EMPTY_CLUSTER_DEFAULTS)){
      to.clusterDefaults = ClusterDefaults.builder()
        .setServices(mergeSet(to.clusterDefaults.services, from.clusterDefaults.services))
        .setConfig(mergeConfig(to.clusterDefaults.config, from.clusterDefaults.config, immutables, isImmutable))
        .setDNSSuffix(mergeString(to.clusterDefaults.dnsSuffix, from.clusterDefaults.dnsSuffix))
        .setHardwaretype(mergeString(to.clusterDefaults.hardwaretype, from.clusterDefaults.hardwaretype))
        .setImagetype(mergeString(to.clusterDefaults.imagetype, from.clusterDefaults.imagetype))
        .setProvider(mergeString(to.clusterDefaults.provider, from.clusterDefaults.provider))
        .build();
    }

    //merge compatibilities services
    if(!from.compatibilities.equals(Compatibilities.EMPTY_COMPATIBILITIES)) {
      to.compatibilities = Compatibilities.builder()
        .setServices(mergeSet(to.compatibilities.services, from.compatibilities.services))
        .setHardwaretypes(mergeSet(to.compatibilities.hardwaretypes, from.compatibilities.hardwaretypes))
        .setImagetypes(mergeSet(to.compatibilities.imagetypes, from.compatibilities.imagetypes))
        .build();
    }
  }

  //for parent extension
  private void copyFullProps(AbstractTemplate from, AbstractTemplate to, Set<String> immutables)
    throws TemplateImmutabilityException {
    copyMainProps(from, to, immutables, false);

    //merge constraints
    if(!from.constraints.equals(Constraints.EMPTY_CONSTRAINTS)) {
      Map<String, ServiceConstraint> serviceConstraint = mergeMap(to.constraints.serviceConstraints,
                                                                  from.constraints.serviceConstraints);
      Set<Set<String>> servicesThatMustCoexist = mergeSet(to.constraints.layoutConstraint.servicesThatMustCoexist,
                                                          from.constraints.layoutConstraint.servicesThatMustCoexist);
      Set<Set<String>> servicesThatMustNotCoexist = mergeSet(to.constraints.layoutConstraint.servicesThatMustNotCoexist,
                                                             from.constraints.layoutConstraint.servicesThatMustNotCoexist);

      to.constraints = new Constraints(serviceConstraint,
                                       new LayoutConstraint(servicesThatMustCoexist, servicesThatMustNotCoexist),
                                       from.constraints.sizeConstraint);
    }

    //merge admin lease duration
    if (!from.administration.equals(Administration.EMPTY_ADMINISTRATION)) {
      to.administration = from.administration;
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

  private JsonObject mergeConfig(JsonObject to, JsonObject from, Set<String> immutables, boolean isImmutable) throws TemplateImmutabilityException {
    //merge defaults config
    JsonObject finalConfig = new JsonObject();
    for (Map.Entry<String, JsonElement> config : to.entrySet()) {
      finalConfig.add(config.getKey(), config.getValue());
    }
    for (Map.Entry<String, JsonElement> fromConfigItem : from.entrySet()) {
      String name = fromConfigItem.getKey();
      JsonElement value = fromConfigItem.getValue();
      if (immutables.contains(name) && finalConfig.has(name)) {
        throw new TemplateImmutabilityException(name + " can't be overridden due immutability.");
      }
      finalConfig.add(name, value);
      if (isImmutable) {
        immutables.add(name);
      }
    }
    return finalConfig;
  }

  private String mergeString(String to, String from) {
    return isBlank(from) ? to : from;
  }
}
