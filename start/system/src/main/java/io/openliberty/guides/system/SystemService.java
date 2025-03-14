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
package io.openliberty.guides.system;

import io.openliberty.guides.models.PropertyMessage;
import io.openliberty.guides.models.SystemLoad;
import io.opentelemetry.api.internal.StringUtils;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.enterprise.context.ApplicationScoped;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;

@ApplicationScoped
public class SystemService {
  private static final OperatingSystemMXBean OS_MEAN = ManagementFactory.getOperatingSystemMXBean();
  private final Logger logger = Logger.getLogger(SystemService.class.getName());
  private static String hostname = null;

  private static String getHostname() {
    if (hostname == null) {
      try {
        return InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        return System.getenv("HOSTNAME");
      }
    }
    return hostname;
  }

  @Outgoing("systemLoad")
  public Publisher<SystemLoad> sendSystemLoad() {
    return Flowable.interval(15, TimeUnit.SECONDS)
        .map(interval -> new SystemLoad(getHostname(), OS_MEAN.getSystemLoadAverage()));
  }

  @Incoming("propertyRequest")
  @Outgoing("propertyResponse")
  public PropertyMessage sendProperty(String propertyName) {
    logger.info("sendProperty: " + propertyName);
    if (StringUtils.isNullOrEmpty(propertyName)) {
      logger.warning(propertyName == null ? "Null" : "An empty string" + "is not System Property");
      return null;
    }

    return new PropertyMessage(
        getHostname(), propertyName, System.getProperty(propertyName, "unknown"));
  }
}
