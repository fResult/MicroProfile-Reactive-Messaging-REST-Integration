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
package io.openliberty.guides.system.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.KafkaFuture;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class SystemReadinessCheck implements HealthCheck {

  private static Logger logger = Logger.getLogger(SystemReadinessCheck.class.getName());

  @Inject
  @ConfigProperty(name = "mp.messaging.connector.liberty-kafka.bootstrap.servers")
  String kafkaServer;

  @Override
  public HealthCheckResponse call() {
    boolean up = isReady();
    return HealthCheckResponse.named(this.getClass().getSimpleName()).status(up).build();
  }

  private boolean isReady() {
    AdminClient adminClient = createAdminClient();
    return checkIfBarConsumerGroupRegistered(adminClient);
  }

  private AdminClient createAdminClient() {
    Properties connectionProperties = new Properties();
    connectionProperties.put("bootstrap.servers", kafkaServer);
    AdminClient adminClient = AdminClient.create(connectionProperties);
    return adminClient;
  }

  private boolean checkIfBarConsumerGroupRegistered(AdminClient adminClient) {
    ListTopicsResult topics = adminClient.listTopics();
    KafkaFuture<Collection<TopicListing>> topicsFuture = topics.listings();
    try {
      Collection<TopicListing> topicList = topicsFuture.get();
      for (TopicListing t : topicList) {
        logger.info("topic: " + t.name());
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
