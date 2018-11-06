package com.leaves.app.shareme.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.leaves.app.shareme.R
import com.leaves.app.shareme.net.adapter.RetrofitAdapter
import com.leaves.app.shareme.net.api.LoginApi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {

    @BindView(R.id.et_account)
    lateinit var accountEditText: EditText

    @BindView(R.id.et_password)
    lateinit var passwordEditText: EditText

    @BindView(R.id.bt_login)
    lateinit var loginBtn: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        ButterKnife.bind(this)
    }

    @OnClick(R.id.bt_login)
    fun login() {
        var account = accountEditText.text.toString()
        var password = passwordEditText.text.toString()
        val disposable = RetrofitAdapter.get().create(LoginApi::class.java)
                .login(account, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                    Log.e("LAZY", it)
                }, {
                    Log.e("LAZY", (it as HttpException).response().toString())
                })
    }
}
