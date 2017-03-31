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
package com.github.pwittchen.reactivenetwork.library.network.observing.strategy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Looper;
import android.util.Log;
import com.github.pwittchen.reactivenetwork.library.Connectivity;
import com.github.pwittchen.reactivenetwork.library.network.observing.NetworkObservingStrategy;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Action;

import static com.github.pwittchen.reactivenetwork.library.ReactiveNetwork.LOG_TAG;

/**
 * Network observing strategy for Android devices before Lollipop (API 20 or lower).
 * Uses Broadcast Receiver.
 */
public class PreLollipopNetworkObservingStrategy implements NetworkObservingStrategy {

  @Override public Observable<Connectivity> observeNetworkConnectivity(final Context context) {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

    return Observable.create(new ObservableOnSubscribe<Connectivity>() {
      @Override public void subscribe(final ObservableEmitter<Connectivity> subscriber)
          throws Exception {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override public void onReceive(Context context, Intent intent) {
            subscriber.onNext(Connectivity.create(context));
          }
        };

        context.registerReceiver(receiver, filter);

        unsubscribeInUiThread(new Action() {
          @Override public void run() {
            tryToUnregisterReceiver(context, receiver);
          }
        });
      }
    }).defaultIfEmpty(Connectivity.create());
  }

  private void tryToUnregisterReceiver(final Context context, final BroadcastReceiver receiver) {
    try {
      context.unregisterReceiver(receiver);
    } catch (Exception exception) {
      onError("receiver was already unregistered", exception);
    }
  }

  @Override public void onError(final String message, final Exception exception) {
    Log.e(LOG_TAG, message, exception);
  }

  private Disposable unsubscribeInUiThread(final Action unsubscribe) {
    return Disposables.fromAction(new Action() {
      @Override public void run() throws Exception {
        if (Looper.getMainLooper() == Looper.myLooper()) {
          unsubscribe.run();
        } else {
          final Scheduler.Worker inner = AndroidSchedulers.mainThread().createWorker();
          inner.schedule(new Runnable() {
            @Override public void run() {
              try {
                unsubscribe.run();
              } catch (Exception e) {
                e.printStackTrace();
              }
              inner.dispose();
            }
          });
        }
      }
    });
  }
}
