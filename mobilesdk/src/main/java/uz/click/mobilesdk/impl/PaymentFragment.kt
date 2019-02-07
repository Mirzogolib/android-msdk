package uz.click.mobilesdk.impl

import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import kotlinx.android.synthetic.main.fragment_payment.*
import uz.click.mobilesdk.BuildConfig
import uz.click.mobilesdk.R
import uz.click.mobilesdk.core.ClickMerchantConfig
import uz.click.mobilesdk.core.ClickMerchantManager
import uz.click.mobilesdk.core.callbacks.ClickMerchantListener
import uz.click.mobilesdk.core.callbacks.ResponseListener
import uz.click.mobilesdk.core.data.CardPaymentResponse
import uz.click.mobilesdk.core.data.CheckoutResponse
import uz.click.mobilesdk.core.data.InitialResponse
import uz.click.mobilesdk.core.data.InvoiceResponse
import uz.click.mobilesdk.core.errors.ArgumentEmptyException
import uz.click.mobilesdk.impl.paymentoptions.PaymentOption
import uz.click.mobilesdk.impl.paymentoptions.PaymentOptionEnum
import uz.click.mobilesdk.utils.*
import java.util.*


class PaymentFragment : AppCompatDialogFragment() {

    private lateinit var config: ClickMerchantConfig
    private var listener: ClickMerchantListener? = null
    var requestId: String = ""
    private var mode = PaymentOptionEnum.USSD
    private lateinit var locale: Locale
    private val clickMerchantManager = ClickMerchantManager()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_payment, container, false)
    }
    init {
        setStyle(STYLE_NO_FRAME, R.style.cl_FullscreenDialogTheme)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener = (parentFragment as MainDialogFragment?)?.getListener()

        if (arguments != null) {
            config = arguments!!.getSerializable(MainDialogFragment.CLICK_MERCHANT_CONFIG) as ClickMerchantConfig
            tvTitle.text = config.productName
            tvSubtitle.text = config.productDescription
            tvSum.text = config.amount.formatDecimals()
            requestId = config.requestId
            mode = config.paymentOption

            locale = Locale(config.locale.toLowerCase())

            btnChange.text = LanguageUtils.getLocaleStringResource(locale, R.string.change, context!!)
            tvToPay.text = LanguageUtils.getLocaleStringResource(locale, R.string.payment, context!!)
            tvNext.text = LanguageUtils.getLocaleStringResource(locale, R.string.next, context!!)
            tvAbbr.text = LanguageUtils.getLocaleStringResource(locale, R.string.abbr, context!!)
            tvRetry.text = LanguageUtils.getLocaleStringResource(locale, R.string.retry, context!!)
            tvErrorText.text =
                LanguageUtils.getLocaleStringResource(locale, R.string.connection_problem, context!!)

            when (mode) {
                PaymentOptionEnum.BANK_CARD -> {
                    llBankCard.show()
                    llUssd.hide()
                    ivPaymentType.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_cards))
                    tvPaymentTypeTitle.text =
                        LanguageUtils.getLocaleStringResource(locale, R.string.bank_card, context!!)
                    tvPaymentTypeSubtitle.text =
                        LanguageUtils.getLocaleStringResource(locale, R.string.card_props, context!!)
                }
                PaymentOptionEnum.USSD -> {
                    llBankCard.hide()
                    llUssd.show()
                    ivPaymentType.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_880))
                    tvPaymentTypeTitle.text =
                        LanguageUtils.getLocaleStringResource(locale, R.string.invoicing, context!!)
                    tvPaymentTypeSubtitle.text = LanguageUtils.getLocaleStringResource(
                        locale,
                        R.string.sms_confirmation,
                        context!!
                    )
                }
            }

            init()
        } else throw ArgumentEmptyException()

        tvRetry.setOnClickListener {
            init()
        }

        etCardNumber.addTextChangedListener(object : CardNumberFormatWatcher(etCardNumber) {
            override fun afterTextWithoutPattern(cardNumber: String) {

            }
        })

        etCardDate.addTextChangedListener(object : CardExpiryDateFormatWatcher(etCardDate) {
            override fun afterTextWithoutPattern(expiredDate: String) {

            }

        })

        etMobileNumber.addTextChangedListener(PhoneNumberTextWatcher(etMobileNumber))

        etMobileNumber.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            viewMobileNumberUnderline.isEnabled = !hasFocus
        }

        llChange.setOnClickListener {
            it.hideKeyboard()
            tvError.hide()
            parentFragment?.let {
                val parent = parentFragment as MainDialogFragment
                parent.onChange()
            }
        }

        etMobileNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                buyClick()
                etMobileNumber.hideKeyboard()
            }
            true
        }

        etCardDate.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                buyClick()
                etCardDate.hideKeyboard()
            }
            true
        }

        btnNext.setOnClickListener {
            buyClick()
            btnNext.hideKeyboard()
        }

        ivScanner.setOnClickListener {
            parentFragment?.let {
                val parent = parentFragment as MainDialogFragment
                parent.scanCard()
            }
        }
    }

    private fun init() {
        if (requestId.isEmpty()) {
            showLoading()
            clickMerchantManager.sendInitialRequest(
                config.serviceId,
                config.merchantId,
                config.amount,
                config.transactionParam,
                config.communalParam,
                config.merchantUserId,
                config.locale, object : ResponseListener<InitialResponse> {
                    override fun onFailure(e: Exception) {
                        e.printStackTrace()
                        showError()
                        if (ErrorUtils.isApiError(e)) {
                            activity?.runOnUiThread {
                                tvErrorText.text = ErrorUtils.getErrorMessage(e, locale, context!!)
                            }
                        }
                    }

                    override fun onSuccess(response: InitialResponse) {
                        showResult()
                        requestId = response.requestId
                        listener?.onReceiveRequestId(requestId)
                    }
                }
            )
        } else {
            showLoading()
            clickMerchantManager.checkPaymentByRequestId(requestId, object : ResponseListener<CheckoutResponse> {
                override fun onFailure(e: Exception) {
                    e.printStackTrace()
                    showError()
                }

                override fun onSuccess(response: CheckoutResponse) {
                    if (response.payment.paymentStatusDescription != null) {
                        when (config.paymentOption) {
                            PaymentOptionEnum.BANK_CARD -> {
                                when {
                                    response.payment.paymentStatus == 1 || response.payment.paymentStatus == 0-> {
                                        parentFragment?.let {
                                            val parent = parentFragment as MainDialogFragment
                                            parent.openPaymentConfirmation(null, requestId)
                                        }
                                    }
                                    response.payment.paymentStatus == 2 || response.payment.paymentStatus < 0 -> {
                                        parentFragment?.let {
                                            val parent = parentFragment as MainDialogFragment
                                            parent.openPaymentResultPage(response.payment)
                                        }
                                    }

                                }
                            }
                            PaymentOptionEnum.USSD -> {
                                when {
                                    response.payment.paymentStatus == 1 || response.payment.paymentStatus == 0 -> {
                                        parentFragment?.let {
                                            val parent = parentFragment as MainDialogFragment
                                            parent.openInvoiceConfirmationPage(requestId)
                                        }
                                    }
                                    response.payment.paymentStatus == 2 || response.payment.paymentStatus < 0 -> {
                                        parentFragment?.let {
                                            val parent = parentFragment as MainDialogFragment
                                            parent.openPaymentResultPage(response.payment)
                                        }
                                    }
                                }
                            }
                        }
                    } else showResult()
                }
            })
        }
    }

    private fun buyClick() {
        tvError.hide()
        when (mode) {
            PaymentOptionEnum.BANK_CARD -> {
                when {
                    etCardNumber.text.toString().isEmpty() -> etCardNumber.error =
                        LanguageUtils.getLocaleStringResource(locale, R.string.enter_card_number, context!!)
                    etCardDate.text.toString().isEmpty() -> etCardDate.error =
                        LanguageUtils.getLocaleStringResource(locale, R.string.enter_expire_date, context!!)
                    else -> {
                        showLoading()
                        clickMerchantManager.paymentByCard(
                            requestId,
                            etCardNumber.text.toString().replace(" ", ""),
                            etCardDate.text.toString().replace("/", ""),
                            object : ResponseListener<CardPaymentResponse> {
                                override fun onFailure(e: Exception) {
                                    showResult()
                                    showErrorMessage(e)
                                }

                                override fun onSuccess(response: CardPaymentResponse) {
                                    showResult()
                                    parentFragment?.let {
                                        val parent = it as MainDialogFragment

                                        val prefs =
                                            context?.getSharedPreferences(BuildConfig.BASE_XML, Context.MODE_PRIVATE)
                                        prefs?.edit()?.putString(requestId, response.phoneNumber)?.apply()

                                        parent.openPaymentConfirmation(response, requestId)
                                    }
                                }
                            }
                        )
                    }
                }
            }
            PaymentOptionEnum.USSD -> {
                if (etMobileNumber.text.toString().isNotEmpty()) {
                    showLoading()
                    clickMerchantManager.paymentByUSSD(
                        requestId,
                        unmaskMobileNumber(etMobileNumber.text.toString()),
                        object : ResponseListener<InvoiceResponse> {
                            override fun onFailure(e: Exception) {
                                showResult()
                                e.printStackTrace()
                                showErrorMessage(e)
                            }

                            override fun onSuccess(response: InvoiceResponse) {
                                showResult()
                                parentFragment?.let {
                                    val parent = it as MainDialogFragment
                                    parent.openInvoiceConfirmationPage(requestId)
                                }
                            }
                        })
                } else etMobileNumber.error =
                    LanguageUtils.getLocaleStringResource(locale, R.string.enter_valid_phone_number, context!!)
            }
        }
    }

    private fun unmaskMobileNumber(number: String): String {
        return "+998" + number.replace(" ", "")
    }


    private fun showErrorMessage(e: Exception) {
        e.printStackTrace()
        if (ErrorUtils.isApiError(e)) {
            activity?.runOnUiThread {
                tvError.show()
                tvError.text = ErrorUtils.getErrorMessage(
                    e,
                    locale,
                    context!!
                )
            }
        } else activity?.runOnUiThread {
            tvError.show()
            tvError.text = LanguageUtils.getLocaleStringResource(
                locale, R.string.network_connection_error, context
                !!
            )
        }
    }

    private fun showLoading() {
        activity?.runOnUiThread {
            llPaymentContainer.show()
            pbLoading.show()
            llBottomContainer.invisible()
            llError.hide()
        }
    }

    private fun showResult() {
        activity?.runOnUiThread {
            llPaymentContainer.show()
            llBottomContainer.show()
            pbLoading.invisible()
            llError.hide()
        }
    }

    private fun showError() {
        activity?.runOnUiThread {
            llPaymentContainer.hide()
            llError.show()
            pbLoading.hide()
        }
    }

    fun paymentOptionSelected(
        item: PaymentOption
    ) {
        mode = item.type
        ivPaymentType.setImageDrawable(ContextCompat.getDrawable(context!!, item.image))
        tvPaymentTypeTitle.text = item.title
        tvPaymentTypeSubtitle.text = item.subtitle
        when (item.type) {
            PaymentOptionEnum.BANK_CARD -> {
                llBankCard.show()
                llUssd.hide()
            }
            PaymentOptionEnum.USSD -> {
                llBankCard.hide()
                llUssd.show()
            }
        }
    }

    fun setScannedData(number: String, date: String) {
        etCardDate.setText(date)
        etCardNumber.setText(number)
    }
}