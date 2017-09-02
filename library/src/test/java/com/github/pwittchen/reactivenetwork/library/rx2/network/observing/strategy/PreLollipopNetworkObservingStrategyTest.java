/*
 * Copyright (C) 2016 Piotr Wittchen
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
package com.github.pwittchen.reactivenetwork.library.rx2.network.observing.strategy;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.NetworkInfo;
import com.github.pwittchen.reactivenetwork.library.rx2.BuildConfig;
import com.github.pwittchen.reactivenetwork.library.rx2.Connectivity;
import com.github.pwittchen.reactivenetwork.library.rx2.network.observing.NetworkObservingStrategy;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// We are suppressing PMD here because we want static imports in unit tests
@SuppressWarnings("PMD") @RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class) public class PreLollipopNetworkObservingStrategyTest {

  @Rule public MockitoRule rule = MockitoJUnit.rule();
  @Spy private PreLollipopNetworkObservingStrategy strategy =
      new PreLollipopNetworkObservingStrategy();
  @Mock private BroadcastReceiver broadcastReceiver;

  @Test public void shouldObserveConnectivity() {
    // given
    final NetworkObservingStrategy strategy = new PreLollipopNetworkObservingStrategy();
    final Context context = RuntimeEnvironment.application.getApplicationContext();

    // when
    strategy.observeNetworkConnectivity(context).subscribe(new Consumer<Connectivity>() {
      @Override public void accept(Connectivity connectivity) {

        // then
        assertThat(connectivity.getState()).isEqualTo(NetworkInfo.State.CONNECTED);
      }
    });
  }

  @Test public void shouldStopObservingConnectivity() {
    // given
    final NetworkObservingStrategy strategy = new PreLollipopNetworkObservingStrategy();
    final Context context = RuntimeEnvironment.application.getApplicationContext();
    final Observable<Connectivity> observable = strategy.observeNetworkConnectivity(context);

    // when
    Disposable disposable = observable.subscribe();
    disposable.dispose();

    // then
    assertThat(disposable.isDisposed()).isTrue();
  }

  @Test public void shouldCallOnError() {
    // given
    final String message = "error message";
    final Exception exception = new Exception();

    // when
    strategy.onError(message, exception);

    // then
    verify(strategy, times(1)).onError(message, exception);
  }

  @Test public void shouldTryToUnregisterReceiver() {
    // given
    final PreLollipopNetworkObservingStrategy preLollipopNetworkObservingStrategy =
        new PreLollipopNetworkObservingStrategy();
    final Application context = spy(RuntimeEnvironment.application);

    // when
    preLollipopNetworkObservingStrategy.tryToUnregisterReceiver(context, broadcastReceiver);

    // then
    verify(context).unregisterReceiver(broadcastReceiver);
  }

  @Test public void shouldTryToUnregisterReceiverAfterDispose() {
    // given
    Context context = RuntimeEnvironment.application.getApplicationContext();

    // when
    Disposable disposable = strategy.observeNetworkConnectivity(context).subscribe();
    disposable.dispose();

    // then
    verify(strategy).tryToUnregisterReceiver(eq(context), any(BroadcastReceiver.class));
  }
}
