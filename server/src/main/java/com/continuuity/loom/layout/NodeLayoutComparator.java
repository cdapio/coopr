/*
 * Copyright 2012-2014, Continuuity, Inc.
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
package com.continuuity.loom.layout;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

/**
 * Comparator for {@link NodeLayout} objects, which allows specifying preferences for hardware types and image types.
 * A node layout with more services than another one is seen as smaller.  If the number of services is the same, the
 * hardware type is compared with the given hardware type preferences.
 * A preferred hardware type means that for node layouts that are exactly the same except for hardware type, the layout
 * with the preferred hardware type will be seen as less than the one with the less preferred hardware type.  If
 * hardware types are of the same preference, image type is used as a tie breaker.  If both image and hardware types
 * are the same preference, they are compare as strings.  If both image and hardware are exactly the same, and the
 * number of services are exactly the same, the service sets are sorted and string comparison is used on each service
 * in order.
 */
public class NodeLayoutComparator implements Comparator<NodeLayout> {
  private final List<String> hardwareTypePreference;
  private final List<String> imageTypePreference;

  public NodeLayoutComparator(List<String> hardwareTypePreference, List<String> imageTypePreference) {
    this.hardwareTypePreference = hardwareTypePreference;
    this.imageTypePreference = imageTypePreference;
  }

  @Override
  public int compare(NodeLayout n1, NodeLayout n2) {
    // first compare by number of services
    SortedSet<String> n1Services = n1.getServiceNames();
    SortedSet<String> n2Services = n2.getServiceNames();
    if (n1Services.size() > n2Services.size()) {
      return -1;
    } else if (n2Services.size() > n1Services.size()) {
      return 1;
    } else {
      // compare hardware types preference first
      int n1HardwareRank = getPreference(n1.getHardwareTypeName(), hardwareTypePreference);
      int n2HardwareRank = getPreference(n2.getHardwareTypeName(), hardwareTypePreference);
      if (n1HardwareRank < n2HardwareRank) {
        return -1;
      } else if (n1HardwareRank > n2HardwareRank) {
        return 1;
      } else {
        // if hardware types have the same preference, compare image type preferences.
        int n1ImageRank = getPreference(n1.getImageTypeName(), imageTypePreference);
        int n2ImageRank = getPreference(n2.getImageTypeName(), imageTypePreference);
        if (n1ImageRank < n2ImageRank) {
          return -1;
        } else if (n1ImageRank > n2ImageRank) {
          return 1;
        } else {
          // if same preference for hardware type and image type, compare by their strings (hw first, img next)
          int strCompare =
            compareStrings(n1.getHardwareTypeName(), n1.getImageTypeName(),
                           n2.getHardwareTypeName(), n2.getImageTypeName());
          if (strCompare != 0) {
            return strCompare;
          }
        }
      }
    }

    // same number of services, same hwtype and imgtype.  compare services alphabetically.
    Iterator<String> iter1 = n1Services.iterator();
    Iterator<String> iter2 = n2Services.iterator();
    while (iter1.hasNext()) {
      String service1 = iter1.next();
      String service2 = iter2.next();
      int compare = service1.compareTo(service2);
      if (compare != 0) {
        return compare;
      }
    }

    return 0;
  }

  private int compareStrings(String hw1, String img1, String hw2, String img2) {
    int compare = hw1.compareTo(hw2);
    if (compare != 0) {
      return compare;
    } else {
      compare = img1.compareTo(img2);
      if (compare != 0) {
        return compare;
      }
    }
    return 0;
  }

  private int getPreference(String val, List<String> preferences) {
    int out = Integer.MAX_VALUE;
    if (preferences != null) {
      out = preferences.indexOf(val);
      if (out < 0) {
        out = Integer.MAX_VALUE;
      }
    }
    return out;
  }
}
