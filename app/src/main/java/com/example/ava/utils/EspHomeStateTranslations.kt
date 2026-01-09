package com.example.ava.utils

import android.content.res.Resources
import com.example.ava.R
import com.example.ava.esphome.Connected
import com.example.ava.esphome.Disconnected
import com.example.ava.esphome.EspHomeState
import com.example.ava.esphome.ServerError
import com.example.ava.esphome.Stopped
import com.example.ava.esphome.voicesatellite.Listening
import com.example.ava.esphome.voicesatellite.Processing
import com.example.ava.esphome.voicesatellite.Responding

fun EspHomeState.translate(resources: Resources): String = when (this) {
    is Stopped -> resources.getString(R.string.satellite_state_stopped)
    is Disconnected -> resources.getString(R.string.satellite_state_disconnected)
    is Connected -> resources.getString(R.string.satellite_state_idle)
    is Listening -> resources.getString(R.string.satellite_state_listening)
    is Processing -> resources.getString(R.string.satellite_state_processing)
    is Responding -> resources.getString(R.string.satellite_state_responding)
    is ServerError -> resources.getString(R.string.satellite_state_server_error, message)
    else -> this.toString()
}