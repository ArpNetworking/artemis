/**
 * Copyright 2016 Brandon Arp
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

import java.util.Comparator;
import java.util.Objects;

/**
 * Compares version strings according to RPM spec.
 *
 * @author Brandon Arp (brandon dot arp at inscopemetrics dot com)
 */
public class RpmVersionComparator implements Comparator<String> {

    // see http://blog.jasonantman.com/2014/07/how-yum-and-rpm-compare-versions/
    // and https://github.com/puppetlabs/puppet/pull/2866/files
    @Override
    public int compare(final String o1, final String o2) {
        if (Objects.equals(o1, o2)) {
            return 0;
        }

        int leftIndex = 0;
        int rightIndex = 0;

        final StringBuilder left = new StringBuilder();
        final StringBuilder right = new StringBuilder();
        while (leftIndex < o1.length() && rightIndex < o2.length()) {
            // Get the next chunk from the strings
            leftIndex = getNextChunk(leftIndex, o1, left);
            rightIndex = getNextChunk(rightIndex, o2, right);

            final String leftString = left.toString();
            final String rightString = right.toString();

            if (leftString.length() == 0 && rightString.length() == 0) {
                return 0;
            }

            if (!leftString.equals(rightString)) {
                if (leftString.charAt(0) == '~' && rightString.charAt(0) == '~') {
                    return Integer.compare(leftString.length(), rightString.length());
                } else if (leftString.charAt(0) == '~') {
                    return -1;
                } else if (rightString.charAt(0) == '~') {
                    return 1;
                } else if (Character.isDigit(leftString.charAt(0)) && Character.isDigit(rightString.charAt(0))) {
                    // Compare by numeric
                    final int compare = Integer.valueOf(leftString).compareTo(Integer.valueOf(rightString));
                    if (compare != 0) {
                        return compare;
                    }
                } else if (Character.isLetter(leftString.charAt(0)) && Character.isLetter(rightString.charAt(0))) {
                    final int compare = leftString.compareTo(rightString);
                    if (compare != 0) {
                        return compare;
                    }
                } else if (Character.isLetter(leftString.charAt(0))) {
                    return -1;
                } else if (Character.isLetter(rightString.charAt(0))) {
                    return 1;
                }
            }
        }

        final int leftRemaining = o1.length() - leftIndex;
        final int rightRemaining = o2.length() - rightIndex;
        if (leftRemaining > 0 && o1.charAt(leftIndex) == '~') {
            return -1;
        } else if (rightRemaining > 0 && o2.charAt(rightIndex) == '~') {
            return 1;
        } else {
            return Integer.compare(leftRemaining, rightRemaining);
        }
    }

    private int getNextChunk(final int initialIndex, final String string, final StringBuilder builder) {
        int index = initialIndex;
        builder.setLength(0);
        char character = string.charAt(index);

        // Strip all the non-alphanumeric and ~ characters
        while (!(Character.isLetterOrDigit(character) || character == '~')) {
            index++;
            if (index >= string.length()) {
                return index;
            }
            character = string.charAt(index);
        }

        if (Character.isLetter(character)) {
            // Consume until non-alpha
            while (Character.isLetter(character)) {
                builder.append(character);
                index++;
                if (index >= string.length()) {
                    return index;
                }
                character = string.charAt(index);
            }
        } else if (Character.isDigit(character)) {
            // Consume until non-numeric
            while (Character.isDigit(character)) {
                builder.append(character);
                index++;
                if (index >= string.length()) {
                    return index;
                }
                character = string.charAt(index);
            }
        } else if (character == '~') {
            // Consume until non-numeric
            while (character == '~') {
                builder.append(character);
                index++;
                if (index >= string.length()) {
                    return index;
                }
                character = string.charAt(index);
            }
        }
        return index;
    }
}
