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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import play.GlobalSettings;
import play.test.FakeApplication;
import play.test.Helpers;
import play.test.WithApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Matthew Hayter (mhayter at groupon dot com)
 */
public class DockerPackageClientTest extends WithApplication {
    @Override
    protected FakeApplication provideFakeApplication() {
        return Helpers.fakeApplication(ImmutableMap.of(
                "logger.application", "DEBUG"
        ), new GlobalSettings());
    }

    @Test
    public void testGetImageVersions() {
        final WireMockServer server = new WireMockServer(8089);
        WireMock.configureFor("localhost", 8089);
        server.start();

        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/v1/search"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody("{\"num_results\": 11, \"query\": \"\", \"results\": [{\"description\": null, \"name\": "
                                + "\"acct/my_app\"}, {\"description\": null, \"name\": \"example/registry_test\"}]}")));

        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/v1/repositories/acct/my_app/tags"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody("{\"latest\": \"dddeea1f029c81c83cdf82fce0048030af355b2edd1795e0f559f264d0a29559\", \"3.1\": \"dddeea1f029c81c83cdf82fce0048030af355b2edd1795e0f559f264d0a29559\"}")));

        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/v1/repositories/example/registry_test/tags"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody("{\"latest\": \"845d6a1f029c81c83cdf82fce0048030af355b2edd1795e0f559f264d0a29559\", \"0.1\": \"ccccca1f029c81c83cdf82fce0048030af355b2edd1795e0f559f264d0a29559\"}")));


        final DockerPackageClient client = new DockerPackageClient("http://localhost:8089");

        final DockerPackageClient.PackageListResponse packageListResponse = client.getAllPackages().get(5, TimeUnit.SECONDS);

        final Map<String, List<DockerPackageClient.ImageMetadata>> pkgMap = packageListResponse.getPackages();

        // We have two repos
        Assert.assertTrue(pkgMap.containsKey("acct/my_app"));
        Assert.assertTrue(pkgMap.containsKey("example/registry_test"));

        // The first has one image with two tags for the same image
        final List<DockerPackageClient.ImageMetadata> myProfileImages = pkgMap.get("acct/my_app");
        // One image:
        Assert.assertEquals(1, myProfileImages.size());
        final DockerPackageClient.ImageMetadata onlyImage = myProfileImages.get(0);
        Assert.assertEquals(2, onlyImage.getTags().size());
        // Image has two tags:
        Assert.assertEquals("dddeea1f029c81c83cdf82fce0048030af355b2edd1795e0f559f264d0a29559", onlyImage.getId());
        Assert.assertTrue(onlyImage.getTags().contains("latest"));
        Assert.assertTrue(onlyImage.getTags().contains("3.1"));
        // Image has repo name included:
        Assert.assertEquals("acct/my_app", onlyImage.getName());


        // The second repo has two images, one tag each.
        final List<DockerPackageClient.ImageMetadata> registryTestImage = pkgMap.get("example/registry_test");
        Assert.assertEquals(2, registryTestImage.size());

        // Order of images is unspecified; clone list in order to sort
        ArrayList<DockerPackageClient.ImageMetadata> sortedRegistryTestImgs = new ArrayList<>(registryTestImage);
        sortedRegistryTestImgs.sort((imageMeta1, imageMeta2) -> imageMeta1.getId().compareTo(imageMeta2.getId()));

        // Image with the tag 'latest':
        final DockerPackageClient.ImageMetadata imageLatest = sortedRegistryTestImgs.get(0);
        Assert.assertEquals(1, imageLatest.getTags().size());
        Assert.assertEquals("845d6a1f029c81c83cdf82fce0048030af355b2edd1795e0f559f264d0a29559", imageLatest.getId());
        Assert.assertTrue(imageLatest.getTags().contains("latest"));

        // Image with the
        final DockerPackageClient.ImageMetadata image01 = sortedRegistryTestImgs.get(1);
        Assert.assertEquals(1, image01.getTags().size());
        Assert.assertEquals("ccccca1f029c81c83cdf82fce0048030af355b2edd1795e0f559f264d0a29559", image01.getId());
        Assert.assertTrue(image01.getTags().contains("0.1"));

        server.stop();
    }
}
