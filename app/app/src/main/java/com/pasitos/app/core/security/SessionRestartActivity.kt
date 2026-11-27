package com.pasitos.app.core.security

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Process
import com.pasitos.app.MainActivity

/** Proceso auxiliar: descarta la memoria de la cuenta antes de abrir la app de nuevo. */
class SessionRestartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val previous = intent.getIntExtra(PREVIOUS_PID, -1)
        if (previous > 0 && previous != Process.myPid()) Process.killProcess(previous)
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
        Process.killProcess(Process.myPid())
    }
    companion object { const val PREVIOUS_PID = "previous_pid" }
}
