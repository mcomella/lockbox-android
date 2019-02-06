/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.action.OnboardingAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.FingerprintStore.AuthenticationState as AuthenticationState
import mozilla.lockbox.view.OnboardingFingerprintAuthFragment.AuthCallback as AuthCallback

interface OnboardingFingerprintView {
    fun onSucceeded()
    fun onFailed(error: String?)
    fun onError(error: String?)
    val onDismiss: Observable<Unit>
    val authCallback: Observable<AuthCallback>
}

@ExperimentalCoroutinesApi
class OnboardingFingerprintAuthPresenter(
    private val view: OnboardingFingerprintView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {

    override fun onViewReady() {
        if (!fingerprintStore.isFingerprintAuthAvailable) {
            dispatcher.dispatch(RouteAction.SkipOnboarding)
        }

        fingerprintStore.authState
            .subscribe(this::updateState)
            .addTo(compositeDisposable)

        view.authCallback
            .subscribe {
                dispatcher.dispatch(FingerprintAuthAction.OnAuthentication(it))
                val unlock = when (it) {
                    is AuthCallback.OnAuth -> true
                    is AuthCallback.OnError -> false
                }
                dispatcher.dispatch(SettingAction.UnlockWithFingerprint(unlock))
                dispatcher.dispatch(RouteAction.ItemList)
            }
            .addTo(compositeDisposable)

        view.onDismiss.subscribe {
            dispatcher.dispatch(OnboardingAction.OnDismiss)
            dispatcher.dispatch(SettingAction.UnlockWithFingerprint(false))
            dispatcher.dispatch(RouteAction.SkipOnboarding)
        }?.addTo(compositeDisposable)
    }

    override fun onResume() {
        super.onResume()
        dispatcher.dispatch(FingerprintSensorAction.Start)
    }

    override fun onPause() {
        super.onPause()
        dispatcher.dispatch(FingerprintSensorAction.Stop)
    }

    private fun updateState(state: FingerprintStore.AuthenticationState) {
        when (state) {
            is AuthenticationState.Succeeded -> view.onSucceeded()
            is AuthenticationState.Failed -> view.onFailed(state.error)
            is AuthenticationState.Error -> view.onError(state.error)
        }
    }
}
