/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.annotation.TargetApi
import android.os.Build
import android.view.autofill.AutofillManager
import androidx.annotation.RequiresApi
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingIntent
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter

interface OnboardingAutofillView {
    val onDismiss: Observable<Unit>
    val onEnable: Observable<Unit>
}

@ExperimentalCoroutinesApi
class OnboardingAutofillPresenter(
    private val view: OnboardingAutofillView,
    private val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {

    @TargetApi(Build.VERSION_CODES.O)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewReady() {

        view.onDismiss.subscribe {
            dispatcher.dispatch(RouteAction.Onboarding.SkipOnboarding)
        }?.addTo(compositeDisposable)

        view.onEnable.subscribe {
            dispatcher.dispatch(RouteAction.SystemSetting(SettingIntent.Autofill))
        }?.addTo(compositeDisposable)
    }
}
