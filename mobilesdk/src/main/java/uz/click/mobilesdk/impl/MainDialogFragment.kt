package uz.click.mobilesdk.impl

import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.BottomSheetDialogFragment
import android.view.*
import android.widget.FrameLayout
import uz.click.mobilesdk.R
import uz.click.mobilesdk.core.ClickMerchantConfig
import uz.click.mobilesdk.core.callbacks.ClickMerchantListener
import uz.click.mobilesdk.core.data.CardPaymentResponse
import uz.click.mobilesdk.core.data.PaymentResponse
import uz.click.mobilesdk.core.errors.ArgumentEmptyException
import uz.click.mobilesdk.impl.paymentoptions.PaymentOption
import uz.click.mobilesdk.impl.paymentoptions.PaymentOptionListFragment

class MainDialogFragment : BottomSheetDialogFragment() {

    private val paymentPage = PaymentFragment::class.java.name
    private val paymentOptions = PaymentOptionListFragment::class.java.name
    private val invoiceConfirmation = InvoiceConfirmationFragment::class.java.name
    private val paymentResult = PaymentResultFragment::class.java.name
    private val paymentConfirmation = PaymentConfirmationFragment::class.java.name
    private val scan = ScanFragment::class.java.name
    private lateinit var config: ClickMerchantConfig
    private var listener: ClickMerchantListener? = null

    companion object {
        const val CLICK_MERCHANT_CONFIG = "CLICK_MERCHANT_CONFIG"
        const val REQUEST_ID = "REQUEST_ID"
        const val PAYMENT_RESULT = "PAYMENT_RESULT"
        const val PAYMENT_AMOUNT = "PAYMENT_AMOUNT"
        const val LOCALE = "LOCALE"

        fun newInstance(config: ClickMerchantConfig?): MainDialogFragment {
            val bundle = Bundle()
            bundle.putSerializable(CLICK_MERCHANT_CONFIG, config)
            val dialog = MainDialogFragment()
            dialog.arguments = bundle
            return dialog
        }
    }

    init {
        setStyle(STYLE_NO_FRAME, R.style.cl_MainDialogTheme)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return inflater.inflate(R.layout.dialog_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments != null) {
            config = arguments!!.getSerializable(CLICK_MERCHANT_CONFIG) as ClickMerchantConfig
        } else throw ArgumentEmptyException()

        val onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            (dialog as? BottomSheetDialog)?.also { dialog ->
                val bottomSheet = dialog.findViewById<FrameLayout?>(android.support.design.R.id.design_bottom_sheet)
                BottomSheetBehavior.from(bottomSheet).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                    isHideable = true
                    peekHeight = 0
                }
            }
        }
        view.viewTreeObserver?.addOnGlobalLayoutListener(onGlobalLayoutListener)

        val transaction = childFragmentManager.beginTransaction()

        val bundle = Bundle()
        bundle.putSerializable(CLICK_MERCHANT_CONFIG, config)
        val payment = PaymentFragment()
        payment.arguments = bundle
        transaction.add(R.id.bottomSheetContainer, payment, paymentPage)
        transaction.commit()
    }

    override fun onResume() {
        super.onResume()
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action != KeyEvent.ACTION_DOWN) {
                    val option = childFragmentManager.findFragmentByTag(paymentOptions) as PaymentOptionListFragment?
                    val scan = childFragmentManager.findFragmentByTag(scan) as ScanFragment?
                    if (option != null) {
                        if (option.isVisible) {
                            childFragmentManager.popBackStack()
                            showPaymentPage()
                        }
                    } else
                        if (scan != null) {
                            if (scan.isVisible) {
                                childFragmentManager.popBackStack()
                                showPaymentPage()
                            }
                        } else dismiss()
                }

                true
            } else
                false
        }
    }

    private fun showPaymentPage() {
        val transaction = childFragmentManager.beginTransaction()
        val payment = childFragmentManager.findFragmentByTag(paymentPage) as PaymentFragment?
        if (payment != null) {
            transaction.show(payment)
        }
        transaction.commit()
    }

    fun onChange() {
        hideFragments()

        val transaction = childFragmentManager.beginTransaction()

        val bundle = Bundle()
        bundle.putString(LOCALE, config.locale)
        val payment = PaymentOptionListFragment()
        payment.arguments = bundle
        transaction.add(
            R.id.bottomSheetContainer,
            payment, paymentOptions
        )
        transaction.addToBackStack(null)
        transaction.commit()
    }

    fun setScannedData(number: String, date: String) {
        childFragmentManager.popBackStack()
        val transaction = childFragmentManager.beginTransaction()
        val payment = childFragmentManager.findFragmentByTag(paymentPage) as PaymentFragment?
        if (payment != null) {
            transaction.show(payment)
            payment.setScannedData(number, date)
        }
        transaction.commit()
    }

    fun scanCard() {
        hideFragments()
        val transaction = childFragmentManager.beginTransaction()
        transaction.add(
            R.id.bottomSheetContainer,
            ScanFragment(), scan
        )
        transaction.addToBackStack(null)
        transaction.commit()
    }

    fun paymentOptionSelected(item: PaymentOption) {
        hideFragments()
        childFragmentManager.popBackStack()

        val transaction = childFragmentManager.beginTransaction()
        val payment = childFragmentManager.findFragmentByTag(paymentPage) as PaymentFragment?
        if (payment != null) {
            transaction.show(payment)
            payment.paymentOptionSelected(item)
        }
        transaction.commit()
    }

    fun openInvoiceConfirmationPage(requestId: String) {
        if (isAdded) {

            val transaction = childFragmentManager.beginTransaction()
            hideFragments()

            val confirm = childFragmentManager.findFragmentByTag(invoiceConfirmation) as InvoiceConfirmationFragment?
            if (confirm != null) {
                transaction.remove(confirm)
            }
            val invoice = InvoiceConfirmationFragment()
            val bundle = Bundle()
            bundle.putString(REQUEST_ID, requestId)
            bundle.putString(LOCALE, config.locale)
            invoice.arguments = bundle
            transaction.add(
                R.id.bottomSheetContainer,
                invoice, invoiceConfirmation
            )

            transaction.commit()
        }
    }

    fun openPaymentResultPage(payment: PaymentResponse) {
        if (isAdded) {
            val transaction = childFragmentManager.beginTransaction()
            hideFragments()

            val paymentResultFragment = childFragmentManager.findFragmentByTag(paymentResult) as PaymentResultFragment?
            if (paymentResultFragment != null) {
                transaction.remove(paymentResultFragment)
            }
            val result = PaymentResultFragment()
            val bundle = Bundle()
            bundle.putSerializable(PAYMENT_RESULT, payment)
            bundle.putDouble(PAYMENT_AMOUNT, config.amount)
            bundle.putString(LOCALE, config.locale)
            result.arguments = bundle
            transaction.add(
                R.id.bottomSheetContainer,
                result,
                paymentResult
            )
            transaction.commitAllowingStateLoss()
        }
    }

    fun openPaymentConfirmation(cardPayment: CardPaymentResponse?, requestId: String) {
        if (isAdded) {
            val transaction = childFragmentManager.beginTransaction()
            hideFragments()

            val confirm = childFragmentManager.findFragmentByTag(paymentConfirmation) as PaymentConfirmationFragment?
            if (confirm != null) {
                transaction.remove(confirm)
            }
            val payment = PaymentConfirmationFragment()
            val bundle = Bundle()
            bundle.putString(LOCALE, config.locale)
            bundle.putSerializable(PAYMENT_RESULT, cardPayment)
            bundle.putString(REQUEST_ID, requestId)
            payment.arguments = bundle
            transaction.add(
                R.id.bottomSheetContainer,
                payment, paymentConfirmation
            )
            transaction.commit()
        }
    }

    fun close() {
        dismiss()
    }

    private fun hideFragments() {
        val transaction = childFragmentManager.beginTransaction()
        for (fragment in childFragmentManager.fragments) {
            transaction.hide(fragment)
        }
        transaction.commitAllowingStateLoss()
    }

    fun setListener(listener: ClickMerchantListener) {
        this.listener = listener
    }

    fun getListener(): ClickMerchantListener? = listener

}