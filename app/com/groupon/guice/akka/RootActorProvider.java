/**
 * Copyright 2015 Groupon.com
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
package com.groupon.guice.akka;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.arpnetworking.commons.akka.GuiceActorCreator;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * Base class for creating a root level actor to reduce boilerplate.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public abstract class RootActorProvider implements Provider<ActorRef> {
    /**
     * Public constructor.
     *
     * @param system the actor system
     * @param injector the guice injector
     * @param clazz the class of actor to create
     * @param name the actor's name
     */
    public RootActorProvider(
            final ActorSystem system,
            final Injector injector,
            final Class<? extends Actor> clazz,
            final String name) {
        _system = system;
        _injector = injector;
        _clazz = clazz;
        _name = name;
    }

    @Override
    public ActorRef get() {
        return _system.actorOf(GuiceActorCreator.props(_injector, _clazz), _name);
    }

    private final ActorSystem _system;
    private final Injector _injector;
    private final Class<? extends Actor> _clazz;
    private final String _name;
}
