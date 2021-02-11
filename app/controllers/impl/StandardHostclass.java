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

import com.google.common.collect.Lists;
import controllers.Hostclass;
import forms.AddHostToHostclass;
import forms.NewHostclass;
import io.ebean.Ebean;
import io.ebean.Transaction;
import models.Host;
import org.webjars.play.WebJarsUtil;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import utils.AuthN;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Controller for Environments.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
@Security.Authenticated(AuthN.class)
public class StandardHostclass extends Controller implements Hostclass {
    /**
     * Public constructor.
     *
     * @param formFactory form factory to create forms
     */
    @Inject
    public StandardHostclass(final FormFactory formFactory, final MessagesApi messagesApi, final WebJarsUtil webJarsUtil) {
        _formFactory = formFactory;
        _messagesApi = messagesApi;
        _webJarsUtil = webJarsUtil;
    }

    @Override
    public CompletionStage<Result> detail(final String name, final Http.Request request) {
        final models.Hostclass hostclass = models.Hostclass.getByName(name);
        if (hostclass == null) {
            return CompletableFuture.completedFuture(notFound());
        } else {
            return CompletableFuture.completedFuture(ok(views.html.hostclass.render(hostclass, AddHostToHostclass.form(_formFactory), request, _webJarsUtil)));
        }
    }

    @Override
    public CompletionStage<Result> newHostclass(final String parentHostclass, final Http.Request request) {
        Form<NewHostclass> form = NewHostclass.form(_formFactory);
        final models.Hostclass parent = models.Hostclass.getByName(parentHostclass);
        if (parent != null) {
            final NewHostclass hostclass = new NewHostclass();
            hostclass.setParent(parent.getId());
            form = form.fill(hostclass);
        }
        return CompletableFuture.completedFuture(ok(views.html.newHostclass.render(form, request, _messagesApi.preferred(request), _webJarsUtil)));
    }

    @Override
    public CompletionStage<Result> addHost(final String hostclassName, final Http.Request request) {
        final Form<AddHostToHostclass> bound = AddHostToHostclass.form(_formFactory).bindFromRequest(request);
        final models.Hostclass hostclass = models.Hostclass.getByName(hostclassName);
        if (hostclass == null) {
            return CompletableFuture.completedFuture(notFound());
        }
        if (bound.hasErrors()) {
            return CompletableFuture.completedFuture(badRequest(views.html.hostclass.render(hostclass, bound, request, _webJarsUtil)));
        } else {
            final AddHostToHostclass addObject = bound.get();
            models.Host host = models.Host.getByName(addObject.getHost());
            if (host == null) {
                host = new Host();
                host.setName(addObject.getHost());
            }
            // TODO(vkoskela): Attempting to move a host should generate a warning. [MAI-?]
            host.setHostclass(hostclass);
            host.save();
            return CompletableFuture.completedFuture(ok(views.html.hostclass.render(hostclass, AddHostToHostclass.form(_formFactory), request, _webJarsUtil)));
        }
    }

    @Override
    public CompletionStage<Result> create(final Http.Request request) {
        final Form<NewHostclass> bound = NewHostclass.form(_formFactory).bindFromRequest(request);
        if (bound.hasErrors()) {
            return CompletableFuture.completedFuture(badRequest(views.html.newHostclass.render(bound, request, _messagesApi.preferred(request), _webJarsUtil)));
        } else {
            try (Transaction transaction = Ebean.beginTransaction()) {
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
                return CompletableFuture.completedFuture(redirect(controllers.routes.Hostclass.detail(hostclass.getName())));
            }
        }
    }

    private final FormFactory _formFactory;
    private final MessagesApi _messagesApi;
    private final WebJarsUtil _webJarsUtil;
}
