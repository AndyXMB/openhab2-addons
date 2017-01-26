/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tivo.internal;

import static org.openhab.binding.tivo.TiVoBindingConstants.THING_TYPE_TIVO;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceRegistry;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.tivo.TiVoBindingConstants;
import org.openhab.binding.tivo.handler.TiVoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * The {@link TiVoHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Jayson Kubilis - Initial contribution
 * @author Andrew Black - minor updates
 */

public class TiVoHandlerFactory extends BaseThingHandlerFactory {
    private Logger logger = LoggerFactory.getLogger(TiVoHandlerFactory.class);
    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_TIVO);
    private DiscoveryServiceRegistry discoveryServiceRegistry;

    /**
     * Sets the discovery service registry.
     *
     * @param discoveryServiceRegistry the new discovery service registry
     */

    protected void setDiscoveryServiceRegistry(DiscoveryServiceRegistry discoveryServiceRegistry) {
        logger.debug("TiVo handler - setDiscoveryServiceRegistry was called");
        this.discoveryServiceRegistry = discoveryServiceRegistry;
    }

    /**
     * Unset discovery service registry.
     *
     * @param discoveryServiceRegistry the discovery service registry
     */

    protected void unsetDiscoveryServiceRegistry(DiscoveryServiceRegistry discoveryServiceRegistry) {
        logger.debug("TiVo handler - unsetDiscoveryServiceRegistry was called");
        this.discoveryServiceRegistry = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory#supportsThingType(org.eclipse.smarthome.core.thing.
     * ThingTypeUID)
     */
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory#createHandler(org.eclipse.smarthome.core.thing.
     * Thing)
     */
    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_TIVO)) {
            return new TiVoHandler(thing, discoveryServiceRegistry);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory#createThing(org.eclipse.smarthome.core.thing.
     * ThingTypeUID, org.eclipse.smarthome.config.core.Configuration, org.eclipse.smarthome.core.thing.ThingUID,
     * org.eclipse.smarthome.core.thing.ThingUID)
     */
    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
            ThingUID bridgeUID) {
        logger.debug("createThing({},{},{},{})", thingTypeUID, configuration, thingUID, bridgeUID);

        if (TiVoBindingConstants.THING_TYPE_TIVO.equals(thingTypeUID)) {

            ThingUID deviceUID = getTivoUID(thingTypeUID, thingUID, configuration);
            logger.debug("creating thing {} from deviceUID: {}", thingTypeUID, deviceUID);
            return super.createThing(thingTypeUID, configuration, deviceUID, null);
        }

        throw new IllegalArgumentException("The thing type {} " + thingTypeUID + " is not supported by the binding.");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory#unregisterHandler(org.eclipse.smarthome.core.
     * thing.Thing)
     */
    @Override
    public void unregisterHandler(Thing thing) {
        logger.debug("TiVo handler - unregisterHandler was called");
        super.unregisterHandler(thing);
    }

    /**
     * Gets the tivo UID.
     *
     * @param thingTypeUID the thing type UID
     * @param thingUID the thing UID
     * @param configuration the configuration
     * @return the tivo UID
     */
    private ThingUID getTivoUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration) {
        if (thingUID == null) {
            String name = (String) configuration.get(TiVoBindingConstants.CONFIG_NAME);
            thingUID = new ThingUID(thingTypeUID, name);
        }
        return thingUID;
    }

}
