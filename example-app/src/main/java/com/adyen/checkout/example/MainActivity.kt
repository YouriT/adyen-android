/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 8/2/2019.
 */

package com.adyen.checkout.example

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.adyen.checkout.base.model.PaymentMethodsApiResponse
import com.adyen.checkout.card.CardConfiguration
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.dropin.DropIn
import com.adyen.checkout.dropin.DropInConfiguration
import com.adyen.checkout.example.api.model.createPaymentMethodsRequest
import com.adyen.checkout.example.arch.PaymentMethodsViewModel
import com.adyen.checkout.googlepay.GooglePayConfiguration
import kotlinx.android.synthetic.main.activity_main.progressBar
import kotlinx.android.synthetic.main.activity_main.startCheckoutButton

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG: String = LogUtil.getTag()
    }

    private lateinit var paymentMethodsViewModel: PaymentMethodsViewModel
    private var isWaitingPaymentMethods = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d(TAG, "onCreate")
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        if (intent.hasExtra(DropIn.RESULT_KEY)) {
            Toast.makeText(this, intent.getStringExtra(DropIn.RESULT_KEY), Toast.LENGTH_SHORT).show()
        }

        paymentMethodsViewModel = ViewModelProviders.of(this).get(PaymentMethodsViewModel::class.java)

        startCheckoutButton.setOnClickListener {
            Logger.d(TAG, "Click")

            val currentResponse = paymentMethodsViewModel.paymentMethodResponseLiveData.value
            if (currentResponse != null) {
                startCheckout(currentResponse)
            } else {
                setLoading(true)
            }
        }

        paymentMethodsViewModel.paymentMethodResponseLiveData.observe(this, Observer {
            if (it != null) {
                Logger.d(TAG, "Got paymentMethods response - oneClick? ${it.storedPaymentMethods?.size ?: 0}")
                if (isWaitingPaymentMethods) {
                    startCheckout(it)
                }
            } else {
                Logger.v(TAG, "API response is null")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        paymentMethodsViewModel.requestPaymentMethods(createPaymentMethodsRequest(this@MainActivity))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        Logger.d(TAG, "onOptionsItemSelected")
        if (item?.itemId == R.id.settings) {
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Logger.d(TAG, "onNewIntent")
        if (intent?.hasExtra(DropIn.RESULT_KEY) == true) {
            Toast.makeText(this, intent.getStringExtra(DropIn.RESULT_KEY), Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCheckout(paymentMethodsApiResponse: PaymentMethodsApiResponse) {
        Logger.d(TAG, "startCheckout")
        setLoading(false)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)

        val googlePayConfig = GooglePayConfiguration.Builder(this@MainActivity, "TestMerchantCheckout").build()

        val cardConfiguration =
            CardConfiguration.Builder(this@MainActivity, BuildConfig.PUBLIC_KEY)
                .setShopperReference(preferences.getString(getString(R.string.shopper_reference_key), BuildConfig.SHOPPER_REFERENCE)
                    ?: BuildConfig.SHOPPER_REFERENCE)
                .build()

        val resultIntent = Intent(this, MainActivity::class.java)
        resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

        val dropInConfiguration = DropInConfiguration.Builder(this@MainActivity, resultIntent, ExampleDropInService::class.java)
            .addCardConfiguration(cardConfiguration)
            .addGooglePayConfiguration(googlePayConfig)
            .build()

        DropIn.startPayment(this@MainActivity, paymentMethodsApiResponse, dropInConfiguration)
    }

    private fun setLoading(isLoading: Boolean) {
        isWaitingPaymentMethods = isLoading
        if (isLoading) {
            startCheckoutButton.visibility = View.GONE
            progressBar.show()
        } else {
            startCheckoutButton.visibility = View.VISIBLE
            progressBar.hide()
        }
    }
}
