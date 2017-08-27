/*
 * Copyright (C) 2017 Piotr Wittchen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pwittchen.reactivenetwork.library.rx2.internet.observing.strategy;

import com.github.pwittchen.reactivenetwork.library.rx2.BuildConfig;
import com.github.pwittchen.reactivenetwork.library.rx2.internet.observing.error.ErrorHandler;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.IOException;
import java.net.HttpURLConnection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class) @Config(constants = BuildConfig.class)
public class WalledGardenInternetObservingStrategyTest {

  private static final int INITIAL_INTERVAL_IN_MS = 0;
  private static final int INTERVAL_IN_MS = 2000;
  private static final int PORT = 80;
  private static final int TIMEOUT_IN_MS = 30;
  @Rule public MockitoRule rule = MockitoJUnit.rule();
  @Spy private WalledGardenInternetObservingStrategy strategy;
  @Mock private ErrorHandler errorHandler;

  private String getHost() {
    return strategy.getDefaultPingHost();
  }

  @Test public void shouldBeConnectedToTheInternet() {
    // given
    when(strategy.isConnected(getHost(), PORT, TIMEOUT_IN_MS, errorHandler)).thenReturn(true);

    // when
    final Observable<Boolean> observable =
        strategy.observeInternetConnectivity(INITIAL_INTERVAL_IN_MS, INTERVAL_IN_MS, getHost(),
            PORT, TIMEOUT_IN_MS, errorHandler);

    boolean isConnected = observable.blockingFirst();

    // then
    assertThat(isConnected).isTrue();
  }

  @Test public void shouldNotBeConnectedToTheInternet() {
    // given
    when(strategy.isConnected(getHost(), PORT, TIMEOUT_IN_MS, errorHandler)).thenReturn(false);

    // when
    final Observable<Boolean> observable =
        strategy.observeInternetConnectivity(INITIAL_INTERVAL_IN_MS, INTERVAL_IN_MS, getHost(),
            PORT, TIMEOUT_IN_MS, errorHandler);

    boolean isConnected = observable.blockingFirst();

    // then
    assertThat(isConnected).isFalse();
  }

  @Test public void shouldBeConnectedToTheInternetViaSingle() {
    // given
    when(strategy.isConnected(getHost(), PORT, TIMEOUT_IN_MS, errorHandler)).thenReturn(true);

    // when
    final Single<Boolean> observable =
        strategy.checkInternetConnectivity(getHost(), PORT, TIMEOUT_IN_MS, errorHandler);

    boolean isConnected = observable.blockingGet();

    // then
    assertThat(isConnected).isTrue();
  }

  @Test public void shouldNotBeConnectedToTheInternetViaSingle() {
    // given
    when(strategy.isConnected(getHost(), PORT, TIMEOUT_IN_MS, errorHandler)).thenReturn(false);

    // when
    final Single<Boolean> observable =
        strategy.checkInternetConnectivity(getHost(), PORT, TIMEOUT_IN_MS, errorHandler);

    boolean isConnected = observable.blockingGet();

    // then
    assertThat(isConnected).isFalse();
  }

  @Test public void shouldCreateHttpUrlConnection() throws IOException {
    // given
    final String parsedDefaultHost = "clients3.google.com";

    // when
    HttpURLConnection connection = strategy.createHttpUrlConnection(getHost(), PORT, TIMEOUT_IN_MS);

    // then
    assertThat(connection).isNotNull();
    assertThat(connection.getURL().getHost()).isEqualTo(parsedDefaultHost);
    assertThat(connection.getURL().getPort()).isEqualTo(PORT);
    assertThat(connection.getConnectTimeout()).isEqualTo(TIMEOUT_IN_MS);
    assertThat(connection.getReadTimeout()).isEqualTo(TIMEOUT_IN_MS);
    assertThat(connection.getInstanceFollowRedirects()).isFalse();
    assertThat(connection.getUseCaches()).isFalse();
  }

  @Test public void shouldHandleAnExceptionWhileCreatingUrlConnection() throws IOException {
    // given
    final String errorMsg = "Could not establish connection with WalledGardenStrategy";
    final IOException givenException = new IOException(errorMsg);
    when(strategy.createHttpUrlConnection(getHost(), PORT, TIMEOUT_IN_MS)).thenThrow(
        givenException);

    // when
    strategy.isConnected(getHost(), PORT, TIMEOUT_IN_MS, errorHandler);

    // then
    verify(errorHandler).handleError(givenException, errorMsg);
  }

  @Test public void shouldNotTransformHttpHost() {
    // given
    final String givenHost = "http://www.website.com";

    // when
    String transformedHost = strategy.adjustHost(givenHost);

    // then
    assertThat(transformedHost).isEqualTo(givenHost);
  }

  @Test public void shouldNotTransformHttpsHost() {
    // given
    final String givenHost = "https://www.website.com";

    // when
    String transformedHost = strategy.adjustHost(givenHost);

    // then
    assertThat(transformedHost).isEqualTo(givenHost);
  }

  @Test public void shouldAddHttpProtocolToHost() {
    // given
    final String givenHost = "www.website.com";
    final String expectedHost = "http://www.website.com";

    // when
    String transformedHost = strategy.adjustHost(givenHost);

    // then
    assertThat(transformedHost).isEqualTo(expectedHost);
  }

  @Test public void shouldAdjustHostWhileCheckingConnectivity() {
    // given
    final String host = getHost();
    when(strategy.isConnected(host, PORT, TIMEOUT_IN_MS, errorHandler)).thenReturn(true);

    // when
    strategy.observeInternetConnectivity(INITIAL_INTERVAL_IN_MS, INTERVAL_IN_MS, host, PORT,
        TIMEOUT_IN_MS, errorHandler).blockingFirst();

    // then
    verify(strategy).adjustHost(host);
  }
}
