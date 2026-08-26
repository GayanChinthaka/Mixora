package com.dpi

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import timber.log.Timber

/**
 * Manages activity lifecycle events and associated logic.
 * Provides hooks for monitoring and responding to activity lifecycle changes.
 */
abstract class ActivityLifecycleManager : BaseLifecycleContentProvider() {

    override fun onCreate(): Boolean {
        val application = getApplication() ?: return true

        if (!onInit(application)) {
            return true
        }

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                this@ActivityLifecycleManager.onActivityCreated(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                this@ActivityLifecycleManager.onActivityStarted(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                this@ActivityLifecycleManager.onActivityResumed(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                this@ActivityLifecycleManager.onActivityPaused(activity)
            }

            override fun onActivityStopped(activity: Activity) {
                this@ActivityLifecycleManager.onActivityStopped(activity)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                this@ActivityLifecycleManager.onActivityDestroyed(activity)
            }
        })

        return true
    }

    protected open fun onInit(application: Application): Boolean = true

    protected open fun onActivityCreated(activity: Activity) {}
    protected open fun onActivityStarted(activity: Activity) {}
    protected open fun onActivityResumed(activity: Activity) {}
    protected open fun onActivityPaused(activity: Activity) {}
    protected open fun onActivityStopped(activity: Activity) {}
    protected open fun onActivityDestroyed(activity: Activity) {}

    companion object {
        @SuppressLint("PrivateApi")
        private fun getApplication(): Application? {
            return try {
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val activityThread = activityThreadClass
                    .getMethod("currentActivityThread")
                    .invoke(null)
                activityThreadClass
                    .getMethod("getApplication")
                    .invoke(activityThread) as? Application
            } catch (e: Exception) {
                Timber.tag("AppUtils").w(e, "Failed to get Application instance")
                null
            }
        }
    }
}
