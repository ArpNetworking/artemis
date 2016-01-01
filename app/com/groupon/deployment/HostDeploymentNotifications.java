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
package com.groupon.deployment;

import models.Host;

/**
 * Notification messages for host deployments.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class HostDeploymentNotifications {
    /**
     * Deployment has started on a host.
     */
    public static final class DeploymentStarted {
        /**
         * Public constructor.
         *
         * @param host host the deployment started on
         */
        public DeploymentStarted(final Host host) {
            _host = host;
        }

        public Host getHost() {
            return _host;
        }

        private final Host _host;
    }

    /**
     * Deployment succeeded on a host.
     */
    public static final class DeploymentSucceeded {
        /**
         * Public constructor.
         *
         * @param host host the deployment succeeded on
         */
        public DeploymentSucceeded(final Host host) {
            _host = host;
        }

        public Host getHost() {
            return _host;
        }

        private final Host _host;
    }

    /**
     * Deployment failed on a host.
     */
    public static final class DeploymentFailed {
        /**
         * Public constructor.
         *
         * @param host host the deployment failed on
         * @param failure the failure exception
         */
        public DeploymentFailed(final Host host, final Throwable failure) {
            _host = host;
            _failure = failure;
        }

        public Throwable getFailure() {
            return _failure;
        }

        public Host getHost() {
            return _host;
        }

        private final Host _host;
        private final Throwable _failure;
    }

    /**
     * Deployment log message.
     */
    public static final class DeploymentLog {
        /**
         * Public constructor.
         *
         * @param host host the message came from
         * @param log the log message
         */
        public DeploymentLog(final Host host, final String log) {
            _host = host;
            _log = log;
        }

        public Host getHost() {
            return _host;
        }

        public String getLog() {
            return _log;
        }

        private final Host _host;
        private final String _log;
    }
}
