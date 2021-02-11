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
package global

import org.slf4j
import org.slf4j.LoggerFactory

import java.util.Locale
import javax.inject.{Inject, Provider}
import play.Logger
import play.api.OptionalDevContext
import play.api.http._
import play.api.mvc.request.RequestTarget
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router
import play.core.WebCommands
import play.core.j.{JavaHandler, JavaHandlerComponents}

/**
 * Handles routing of requests and will redirect config requests.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
class ConfigRedirectRequestHandler @Inject() (webCommands: WebCommands,
                                              optDevContext: OptionalDevContext,
                                              router: Provider[Router],
                                              errorHandler: HttpErrorHandler,
                                              configuration: HttpConfiguration,
                                              filters: HttpFilters,
                                              components: JavaHandlerComponents,
                                              configRouter: config.Routes)

//  extends JavaCompatibleHttpRequestHandler(defaultRouter, errorHandler, configuration, filters, components) {
  extends JavaCompatibleHttpRequestHandler(webCommands, optDevContext, router, errorHandler, configuration, filters, components) {

  val logger = LoggerFactory.getLogger(classOf[ConfigRedirectRequestHandler])

  override def routeRequest(request: RequestHeader): Option[Handler] = {
    val handler: Option[Handler] = if (request.host.toLowerCase(Locale.ENGLISH).startsWith("config")) {
      configRouter.handlerFor(request.withTarget(RequestTarget.apply(request.uri, request.path.replaceFirst("/", configRouter.prefix), request.queryString)))
    } else {
       super.routeRequest(request)
    }

    handler match {
      case Some(javaHandler: JavaHandler) =>
        Some(javaHandler.withComponents(components))
      case other => other
    }
  }

  def printRouter(router: Router): Unit = {
    logger.info(router.documentation.map(x => x._1 + " " + x._2).fold("")((x, y) => x + "\n" + y))
  }
}
