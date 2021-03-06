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
package client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * A client for retreiving information about docker packages.
 *
 * @author Matthew Hayter (mhayter at groupon dot com)
 */
public class DockerPackageClient extends ClientBase {

    /**
     * Public constructor.
     *
     * @param baseUrl the base url of the docker registry
     * @param client ws client to use
     */
    @Inject
    public DockerPackageClient(@Named("DockerRegistryUrl") final String baseUrl, final WSClient client) {
        super(baseUrl, client);
    }

    /**
     * Gets all of the packages in the registry.
     *
     * @return a response with all of the packages
     */
    public CompletionStage<PackageListResponse> getAllPackages() {

        final CompletionStage<List<String>> repoNamesPromise = client()
                .url(uri("/v1/search").toString())
                .get()
                .thenApply(searchResponse -> {
                    // searchResponse = { results: [ {name: "", description: "" }, ... ] }
                    final Spliterator<JsonNode> resultsIter = searchResponse.asJson().get("results").spliterator();

                    return StreamSupport.stream(resultsIter, false)
                            .map(jsonResult -> jsonResult.get("name").asText())
                            .collect(Collectors.toList());

                });

        final CompletionStage<List<WSResponse>> tagListJsonNodes = repoNamesPromise.thenCompose(repoNames -> {

            final List<WSRequest> requests = repoNames.stream()
                    .map(name -> client().url(uri(String.format("/v1/repositories/%s/tags", name)).toString()))
                    .collect(Collectors.toList());

            return concurrentExecute(requests, 5);

        });

        // Combine the repo names and the tag lists into a Map, and package in a PackageListResponse:
        return CompletableFuture.allOf(repoNamesPromise.toCompletableFuture(), tagListJsonNodes.toCompletableFuture())
                .thenApply(v -> {
                    final List<String> repoNames = repoNamesPromise.toCompletableFuture().join();
                    final List<WSResponse> tags = tagListJsonNodes.toCompletableFuture().join();
                    final Map<String, List<ImageMetadata>> packages = Maps.newHashMapWithExpectedSize(repoNames.size());
                    final Iterator<String> repoNamesIter = repoNames.iterator();
                    final Iterator<WSResponse> tagListsIter = tags.iterator();

                    while (repoNamesIter.hasNext() && tagListsIter.hasNext()) {
                        final String nextName = repoNamesIter.next();
                        final WSResponse tagListResponse = tagListsIter.next();
                        final List<ImageMetadata> tagList = repoTagListToImages(tagListResponse, nextName);

                        packages.put(nextName, tagList);
                    }

                    return new PackageListResponse(packages);
                });
    }

    private CompletionStage<List<WSResponse>> concurrentExecute(final List<WSRequest> reqs, final int concurrency) {
        if (concurrency <= 0) {
            throw new IllegalArgumentException("concurrency must be >= 1");
        }

        // Maintains the 'lines' of concurrent requests
        final ArrayList<CompletionStage<WSResponse>> concurrentReqs = Lists.newArrayListWithExpectedSize(concurrency);
        for (int i = 0; i < concurrency; i++) {
            // Dummy promises
            concurrentReqs.add(CompletableFuture.completedFuture(null));
        }

        int concurrentCount = 0;
        final ArrayList<CompletionStage<WSResponse>> results = new ArrayList<>(reqs.size());

        for (final WSRequest next : reqs) {
            final int concurrentIndex = concurrentCount % concurrency;
            final CompletionStage<WSResponse> prev = concurrentReqs.get(concurrentIndex);
            final CompletionStage<WSResponse> responsePromise = prev.thenCompose(ignored -> next.execute());
            results.add(responsePromise);
            concurrentReqs.add(concurrentIndex, responsePromise);

            concurrentCount += 1;
        }

        @SuppressWarnings("rawtypes")
        final CompletableFuture[] futures = results.stream()
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures)
                .thenApply(v -> results.stream()
                        .map(cs -> cs.toCompletableFuture().join())
                        .collect(Collectors.toList()));
    }

    /**
     * Convert a response from a docker registry GET tags call into a list of ImageMetadata objects,
     * effectively inverting the JSON map.
     *
     * @param tagsResponse Convertible to JSON which is expected to look like:
     *  {
     *    "latest": "9e89cc6f0bc3c38722009fe6857087b486531f9a779a0c17e3ed29dae8f12c4f",
     *    "0.1.1":  "b486531f9a779a0c17e3ed29dae8f12c4f9e89cc6f0bc3c38722009fe6857087"
     *  }
     * @return A list of image's metadata
     */
    private List<ImageMetadata> repoTagListToImages(final WSResponse tagsResponse, final String repoName) {
        final JsonNode jsonTagToShaMap = tagsResponse.asJson();
        final Map<String, List<String>> shaToTagList = Maps.newHashMap();

        final Iterator<Map.Entry<String, JsonNode>> tags = jsonTagToShaMap.fields();
        while (tags.hasNext()) {
            final Map.Entry<String, JsonNode> tagShaPair = tags.next();
            final String tagName = tagShaPair.getKey();
            final String sha = tagShaPair.getValue().textValue();
            // Add/create the tag list for this SHA
            if (shaToTagList.containsKey(sha)) {
                shaToTagList.get(sha).add(tagName);
            } else {
                final LinkedList<String> tagList = new LinkedList<>();
                tagList.add(tagName);
                shaToTagList.put(sha, tagList);
            }
        }

        final List<ImageMetadata> images = new LinkedList<>();
        shaToTagList.forEach((sha, tagList) -> images.add(new ImageMetadata(repoName, sha, tagList)));
        return images;
    }

    /**
     * Represents a respons from a docker registry.
     */
    public static class PackageListResponse {
        /**
         * Public constructor.
         * @param packages the packages
         */
        public PackageListResponse(final Map<String, List<ImageMetadata>> packages) {
            _packages = packages;
        }

        public Map<String, List<ImageMetadata>> getPackages() {
            return _packages;
        }

        private final Map<String, List<ImageMetadata>> _packages;
    }

    /**
     * Represents image metadata.
     */
    public static class ImageMetadata {

        /**
         * Public constructor.
         *
         * @param repository the name of the repository
         * @param id the ide of the image
         * @param tags the tags for the image
         */
        public ImageMetadata(final String repository, final String id, final List<String> tags) {
            _repository = repository;
            _id = id;
            _tags = tags;
        }

        public String getName() {
            return _repository;
        }

        public String getId() {
            return _id;
        }

        public List<String> getTags() {
            return Collections.unmodifiableList(_tags);
        }

        private final String _repository;
        private final String _id;
        private final List<String> _tags;
    }
}
