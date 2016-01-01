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
package utils;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigIncludeContext;
import com.typesafe.config.ConfigIncluder;
import com.typesafe.config.ConfigIncluderClasspath;
import com.typesafe.config.ConfigIncluderFile;
import com.typesafe.config.ConfigIncluderURL;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;

import java.io.File;
import java.net.URL;

/**
 * Disallows configuration includes.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class NoIncluder implements ConfigIncluder, ConfigIncluderURL, ConfigIncluderFile, ConfigIncluderClasspath {
    @Override
    public ConfigIncluder withFallback(final ConfigIncluder fallback) {
        return this;
    }

    @Override
    public ConfigObject include(final ConfigIncludeContext context, final String what) {
        throw new IncludesNotAllowedException(getMessage(what));
    }

    private String getMessage(final String what) {
        return "Includes not allowed. Found include of " + what;
    }

    @Override
    public ConfigObject includeResources(final ConfigIncludeContext context, final String what) {
        throw new IncludesNotAllowedException(getMessage(what));
    }

    @Override
    public ConfigObject includeFile(final ConfigIncludeContext context, final File what) {
        throw new IncludesNotAllowedException(getMessage(what.getAbsolutePath()));
    }

    @Override
    public ConfigObject includeURL(final ConfigIncludeContext context, final URL what) {
        throw new IncludesNotAllowedException(getMessage(what.toString()));
    }

    /**
     * Exception indicating that an include is not allowed in the configuration block.
     */
    public static final class IncludesNotAllowedException extends ConfigException {

        private IncludesNotAllowedException(final String message) {
            super(message);
        }

        private IncludesNotAllowedException(final String message, final Throwable cause) {
            super(message, cause);
        }

        private IncludesNotAllowedException(final ConfigOrigin origin, final String message) {
            super(origin, message);
        }

        private IncludesNotAllowedException(final ConfigOrigin origin, final String message, final Throwable cause) {
            super(origin, message, cause);
        }

        private static final long serialVersionUID = 1L;
    }
}
