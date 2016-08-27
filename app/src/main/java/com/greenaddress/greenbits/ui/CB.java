package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.widget.Button;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;


public final class CB {

    public static <T> void after(ListenableFuture<T> f,
                                 FutureCallback<? super T> cb,
                                 Executor executor) {
        Futures.addCallback(f, cb, executor);
    }

    /** A FutureCallback that does nothing by default */
    public static class Op<T> implements FutureCallback<T> {
        protected final Activity mActivity;
        public Op() {
            mActivity = null;
        }
        public Op(final Activity activity) {
            mActivity = activity;
        }

        @Override
        public void onSuccess(final T result) {
            if (mActivity != null)
                mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        onUiSuccess(result);
                    }
                });
        }

        @Override
        public void onFailure(final Throwable t) {
            t.printStackTrace();
            if (mActivity != null)
                mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        onUiFailure(t);
                    }
                });
        }

        public void onUiSuccess(final T result){ /* No-op */ }
        public void onUiFailure(final Throwable t) { /* No-op */ }
    }

    /** A FutureCallback that shows a toast (and optionally
     *  enables a button) on failure
     */
    public static class Toast<T> extends Op<T> {

       final Button mEnabler;

       Toast(final Activity activity) {
           this(activity, null);
       }

       Toast(final Activity activity, final Button enabler) {
           super(activity);
           mEnabler = enabler;
       }

       @Override
       final public void onFailure(final Throwable t) {
           t.printStackTrace();
           UI.toast(mActivity, t, mEnabler);
       }
    }

    /** A runnable that takes 1 argument */
    public interface Runnable1T<T> {
        void run(final T arg);
    }
}
