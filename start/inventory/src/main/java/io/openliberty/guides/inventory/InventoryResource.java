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

import io.openliberty.guides.models.PropertyMessage;
import io.openliberty.guides.models.SystemLoad;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.FlowableOnSubscribe;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;

@ApplicationScoped
@Path("/inventory")
public class InventoryResource {

  private final Logger logger = Logger.getLogger(InventoryResource.class.getName());
  private FlowableEmitter<String> propertyNameEmitter;

  @Inject private InventoryManager manager;

  @GET
  @Path("/systems")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSystems() {
    List<Properties> systems = new ArrayList<>(manager.getSystems().values());
    return Response.status(Response.Status.OK).entity(systems).build();
  }

  @GET
  @Path("/systems/{hostname}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSystem(@PathParam("hostname") String hostname) {
    Optional<Properties> system = manager.getSystem(hostname);
    if (system.isPresent()) {
      return Response.status(Response.Status.OK).entity(system).build();
    }
    return Response.status(Response.Status.NOT_FOUND).entity("hostname does not exist.").build();
  }

  @PUT
  @Path("/data")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.TEXT_PLAIN)
  public Response updateSystemProperty(String propertyName) {
    logger.info("updateSystemProperty: " + propertyName);
    Optional.ofNullable(propertyNameEmitter)
        .ifPresentOrElse(
            emitter -> emitter.onNext(propertyName),
            () -> logger.severe("Property name emitter is not initialized."));

    return Response.status(Response.Status.OK)
        .entity("Request successful for the " + propertyName + " property")
        .build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  public Response resetSystems() {
    manager.resetSystems();
    return Response.status(Response.Status.OK).build();
  }

  @Incoming("systemLoad")
  public void updateStatus(SystemLoad sl) {
    String hostname = sl.hostname;
    if (manager.getSystem(hostname).isPresent()) {
      manager.updateCpuStatus(hostname, sl.loadAverage);
      logger.info("Host " + hostname + " was updated: " + sl);
    } else {
      manager.addSystem(hostname, sl.loadAverage);
      logger.info("Host " + hostname + " was added: " + sl);
    }
  }

  @Incoming("addSystemProperty")
  public void getPropertyMessage(PropertyMessage propertyMessage) {
    logger.info("getPropertyMessage: " + propertyMessage);
    final var hostId = propertyMessage.hostname;

    manager
        .getSystem(hostId)
        .ifPresentOrElse(
            ignored -> {
              manager.updatePropertyMessage(hostId, propertyMessage.key, propertyMessage.value);
              logger.info("Host " + hostId + " was updated: " + propertyMessage);
            },
            () -> {
              manager.addSystem(hostId, propertyMessage.key, propertyMessage.value);
              logger.info("Host " + hostId + " was added: " + propertyMessage);
            });
  }

  @Outgoing("requestSystemProperty")
  public Publisher<String> sendPropertyName() {
    FlowableOnSubscribe<String> assignEmitterOnSubscribe = emitter -> propertyNameEmitter = emitter;
    return Flowable.create(assignEmitterOnSubscribe, BackpressureStrategy.BUFFER);
  }
}
