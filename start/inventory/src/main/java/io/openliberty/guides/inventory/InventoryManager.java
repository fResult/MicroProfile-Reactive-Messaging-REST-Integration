// tag::copyright[]
/* ******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ****************************************************************************** */
// end::copyright[]
package io.openliberty.guides.inventory;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

@ApplicationScoped
public class InventoryManager {

  private Map<String, Properties> systems =
      Collections.synchronizedMap(new TreeMap<String, Properties>());

  public void addSystem(String hostname, Double systemLoad) {
    if (!systems.containsKey(hostname)) {
      Properties p = new Properties();
      p.put("hostname", hostname);
      p.put("systemLoad", systemLoad);
      systems.put(hostname, p);
    }
  }

  public void addSystem(String hostname, String key, String value) {
    if (!systems.containsKey(hostname)) {
      Properties p = new Properties();
      p.put("hostname", hostname);
      p.put("key", value);
      systems.put(hostname, p);
    }
  }

  public void updateCpuStatus(String hostname, Double systemLoad) {
    Optional<Properties> p = getSystem(hostname);
    if (p.isPresent()) {
      if (p.get().getProperty(hostname) == null && hostname != null) {
        p.get().put("systemLoad", systemLoad);
      }
    }
  }

  public void updatePropertyMessage(String hostname, String key, String value) {
    Optional<Properties> p = getSystem(hostname);
    if (p.isPresent()) {
      if (p.get().getProperty(hostname) == null && hostname != null) {
        p.get().put(key, value);
      }
    }
  }

  public Optional<Properties> getSystem(String hostname) {
    Properties p = systems.get(hostname);
    return Optional.ofNullable(p);
  }

  public Map<String, Properties> getSystems() {
    return new TreeMap<>(systems);
  }

  public void resetSystems() {
    systems.clear();
  }
}
