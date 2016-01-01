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

import java.util.Locale
import javax.inject.Inject

import play.Logger
import play.api.http._
import play.api.mvc.{Handler, Headers, RequestHeader}
import play.api.routing.Router
import play.core.j.{JavaHandler, JavaHandlerComponents}

/**
 * Handles routing of requests and will redirect config requests.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
class ConfigRedirectRequestHandler @Inject() (defaultRouter: Router,
                                              errorHandler: HttpErrorHandler,
                                              configuration: HttpConfiguration,
                                              filters: HttpFilters,
                                              components: JavaHandlerComponents,
                                              configRouter: config.Routes)
  extends DefaultHttpRequestHandler(defaultRouter, errorHandler, configuration, filters) {

  override def routeRequest(request: RequestHeader): Option[Handler] = {
    val handler: Option[Handler] = if (request.host.toLowerCase(Locale.ENGLISH).startsWith("config")) {
      configRouter.handlerFor(new PrefixRequest(request, configRouter.prefix))
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
    Logger.info(router.documentation.map(x => x._1 + " " + x._2).fold("")((x, y) => x + "\n" + y))
  }

  class PrefixRequest(wrapped: RequestHeader, prefix: String) extends RequestHeader {
      override def headers: Headers = wrapped.headers
      override def secure: Boolean = wrapped.secure
      override def uri: String = wrapped.uri.replaceFirst("/", prefix)
      override def remoteAddress: String = wrapped.remoteAddress
      override def queryString: Map[String, Seq[String]] = wrapped.queryString
      override def method: String = wrapped.method
      override def path: String = wrapped.path.replaceFirst("/", prefix)
      override def version: String = wrapped.version
      override def tags: Map[String, String] = wrapped.tags
      override def id: Long = wrapped.id
    }
}
