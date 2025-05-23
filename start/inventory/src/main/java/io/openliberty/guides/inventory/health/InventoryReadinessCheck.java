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
package io.openliberty.guides.inventory.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.common.KafkaFuture;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class InventoryReadinessCheck implements HealthCheck {

  private static Logger logger = Logger.getLogger(InventoryReadinessCheck.class.getName());

  @Inject
  @ConfigProperty(name = "mp.messaging.connector.liberty-kafka.bootstrap.servers")
  String kafkaServer;

  @Inject
  @ConfigProperty(name = "mp.messaging.incoming.systemLoad.group.id")
  String groupId;

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

  private boolean checkIfBarConsumerGroupRegistered(final AdminClient adminClient) {
    ListConsumerGroupsResult groupsResult = adminClient.listConsumerGroups();
    KafkaFuture<Collection<ConsumerGroupListing>> consumerGroupsFuture = groupsResult.valid();
    try {
      Collection<ConsumerGroupListing> consumerGroups = consumerGroupsFuture.get();
      for (ConsumerGroupListing g : consumerGroups) {
        logger.info("groupId: " + g.groupId());
      }
      return consumerGroups.stream().anyMatch(group -> group.groupId().equals(groupId));
    } catch (Exception e) {
      return false;
    }
  }
}
