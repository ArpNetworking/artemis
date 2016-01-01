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
package controllers.impl;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Transaction;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import controllers.Hostclass;
import forms.AddHostToHostclass;
import forms.NewHostclass;
import play.data.Form;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.AuthN;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;

/**
 * Controller for Environments.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Security.Authenticated(AuthN.class)
public class StandardHostclass extends Controller implements Hostclass {
    /**
     * Public constructor.
     */
    @Inject
    public StandardHostclass() {
    }

    @Override
    public F.Promise<Result> detail(final String name) {
        final models.Hostclass hostclass = models.Hostclass.getByName(name);
        if (hostclass == null) {
            return F.Promise.pure(notFound());
        } else {
            return F.Promise.pure(ok(views.html.hostclass.render(hostclass, AddHostToHostclass.form())));
        }
    }

    @Override
    public F.Promise<Result> newHostclass(final String parentHostclass) {
        Form<NewHostclass> form = NewHostclass.form();
        final models.Hostclass parent = models.Hostclass.getByName(parentHostclass);
        if (parent != null) {
            final NewHostclass hostclass = new NewHostclass();
            hostclass.setParent(parent.getId());
            form = form.fill(hostclass);
        }
        return F.Promise.pure(ok(views.html.newHostclass.render(form)));
    }

    @Override
    public F.Promise<Result> addHost(final String hostclassName) {
        final Form<AddHostToHostclass> bound = AddHostToHostclass.form().bindFromRequest();
        final models.Hostclass hostclass = models.Hostclass.getByName(hostclassName);
        if (hostclass == null) {
            return F.Promise.pure(notFound());
        }
        if (bound.hasErrors()) {
            return F.Promise.pure(badRequest(views.html.hostclass.render(hostclass, bound)));
        } else {
            final AddHostToHostclass addObject = bound.get();
            final models.Host host = models.Host.getByName(addObject.getHost());
            if (host == null) {
                return F.Promise.pure(badRequest(views.html.hostclass.render(hostclass, bound)));
            }
            // TODO(vkoskela): Attempting to move a host should generate a warning. [MAI-?]
            host.setHostclass(hostclass);
            host.save();
            return F.Promise.pure(ok(views.html.hostclass.render(hostclass, AddHostToHostclass.form())));
        }
    }

    @Override
    public F.Promise<Result> create() {
        final Form<NewHostclass> bound = NewHostclass.form().bindFromRequest();
        if (bound.hasErrors()) {
            return F.Promise.pure(badRequest(views.html.newHostclass.render(bound)));
        } else {
            try (final Transaction transaction = Ebean.beginTransaction()) {
                final models.Hostclass hostclass = new models.Hostclass();
                final NewHostclass newHostclass = bound.get();
                hostclass.setName(newHostclass.getName());
                final List<models.Host> hosts = Lists.newArrayList();
                if (newHostclass.getHosts() != null) {
                    for (final Long host : newHostclass.getHosts()) {
                        hosts.add(models.Host.getById(host));
                    }
                }
                hostclass.setHosts(hosts);

                if (newHostclass.getParent() != null) {
                    final models.Hostclass parent = models.Hostclass.getById(newHostclass.getParent());
                    hostclass.setParent(parent);
                    hostclass.setName(String.format("%s/%s", parent.getName(), hostclass.getName()));
                }
                hostclass.save();

                transaction.commit();
                return F.Promise.pure(redirect(controllers.routes.Hostclass.detail(hostclass.getName())));
            } catch (final IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }
}
